package com.scarlett.yuedujilu.ui.scan

import android.app.Application
import com.google.android.gms.tasks.TaskCompletionSource
import com.scarlett.yuedujilu.data.ShowApiClient
import com.scarlett.yuedujilu.data.ShowApiResult
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.concurrent.*
import java.util.concurrent.atomic.*

/** GMS scheduling regression, not a physical camera test. Includes reproductions of the old unsafe ownership. */
@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[35])
class ScannerConcurrencyRegressionTest {
 @Test fun actualGmsTaskRejectsLateCompletionAfterScannerExecutorShutdown() {
  val executor=Executors.newSingleThreadExecutor()
  val pending=TaskCompletionSource<String>()
  val disposed=AtomicBoolean(false);val consumerCalls=AtomicInteger()
  // Exact scheduling primitive used in CameraX1.5.2 MlKitAnalyzer.java242-257.
  pending.task.addOnCompleteListener(executor) { if(!disposed.get()) consumerCalls.incrementAndGet() }
  disposed.set(true);executor.shutdown()
  val failure=assertThrows(RejectedExecutionException::class.java) { pending.setResult("test-only-result") }
  assertEquals(0,consumerCalls.get())
  println("REPRO late completion: "+failure.javaClass.name+"; disposed guard never reached")
 }
 @Test fun queuedMlKitStyleCompletionCanCrashWorkerAfterShutdown() {
  val blocked=CountDownLatch(1);val release=CountDownLatch(1);val uncaught=CountDownLatch(1)
  val error=AtomicReference<Throwable>();val consumerCalls=AtomicInteger()
  val executor=Executors.newSingleThreadExecutor { r -> Thread(r,"isbn-diagnostic-only").apply {
   uncaughtExceptionHandler=Thread.UncaughtExceptionHandler { _,e -> error.set(e);uncaught.countDown() }
  } }
  try {
   executor.execute { blocked.countDown();release.await() };assertTrue(blocked.await(2,TimeUnit.SECONDS))
   val pending=TaskCompletionSource<String>()
   pending.task.addOnCompleteListener(executor) {
    // MlKitAnalyzer.java255 recursively reaches java221: another execute on same executor.
    executor.execute { consumerCalls.incrementAndGet() }
   }
   pending.setResult("test-only-result");executor.shutdown();release.countDown()
   assertTrue(uncaught.await(2,TimeUnit.SECONDS));assertTrue(error.get() is RejectedExecutionException)
   assertEquals(0,consumerCalls.get())
   println("REPRO queued completion: uncaught "+error.get().javaClass.name+" on analysis worker")
  } finally { release.countDown();executor.shutdownNow();executor.awaitTermination(2,TimeUnit.SECONDS) }
 }
 @Test fun lateCompletionUsesMainAfterWorkerShutdownAndDisposedGuardRuns() {
  val app=androidx.test.core.app.ApplicationProvider.getApplicationContext<Application>()
  val main=androidx.core.content.ContextCompat.getMainExecutor(app)
  val worker=Executors.newSingleThreadExecutor();val disposed=AtomicBoolean(false);val calls=AtomicInteger();val guards=AtomicInteger()
  val pending=TaskCompletionSource<String>()
  pending.task.addOnCompleteListener(main){ main.execute { guards.incrementAndGet();if(!disposed.get())calls.incrementAndGet() } }
  disposed.set(true);worker.shutdown();pending.setResult("test-only-result")
  org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
  assertEquals(1,guards.get());assertEquals(0,calls.get());assertTrue(worker.isShutdown)
 }
 @Test fun queuedSuccessAndFailureRemainSafeAcrossExit() {
  val app=androidx.test.core.app.ApplicationProvider.getApplicationContext<Application>()
  val main=androidx.core.content.ContextCompat.getMainExecutor(app);val calls=AtomicInteger();val disposed=AtomicBoolean(false)
  val ok=TaskCompletionSource<String>();val fail=TaskCompletionSource<String>()
  listOf(ok,fail).forEach{it.task.addOnCompleteListener(main){main.execute{if(!disposed.get())calls.incrementAndGet()}}}
  ok.setResult("test-only-result");fail.setException(IllegalStateException("test-only-failure"));disposed.set(true)
  org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(0,calls.get())
 }
 @Test fun productionWiresCompletionToMainAndRetainsOriginalCoordinates() {
  val source=java.io.File("src/main/kotlin/com/scarlett/yuedujilu/ui/scan/ScanIsbnScreen.kt").readText()
  assertTrue(source.contains("MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, mainExec)"))
  assertTrue(source.indexOf("analysis.clearAnalyzer()")<source.indexOf("executor.shutdown()"))
  assertTrue(source.contains("provider?.unbind(preview, analysis)"));assertFalse(source.contains("unbindAll()"))
 }
 @Test fun nullAndWrongJsonShapesDoNotEscapeProductionParser() {
  val client=ShowApiClient()
  val bodies=listOf("not-json","null","[]","{}",
   "{\"showapi_res_code\":null}","{\"showapi_res_code\":[]}",
   "{\"showapi_res_code\":0,\"showapi_res_body\":null}",
   "{\"showapi_res_code\":0,\"showapi_res_body\":[]}",
   "{\"showapi_res_code\":0,\"showapi_res_body\":{\"ret_code\":0,\"data\":null}}",
   "{\"showapi_res_code\":0,\"showapi_res_body\":{\"ret_code\":0,\"data\":{\"title\":[]}}}")
  for(body in bodies) { val r=client.parse(body,200);assertTrue(r is ShowApiResult.Failed || r is ShowApiResult.NotFound) }
  val ok=client.parse("{\"showapi_res_code\":0,\"showapi_res_body\":{\"ret_code\":0,\"data\":{\"title\":\"测试夹具\",\"author\":null,\"page\":null}}}",200)
  assertTrue(ok is ShowApiResult.Ok);ok as ShowApiResult.Ok
  assertEquals("",ok.book.author);assertNull(ok.book.page)
  println("PARSER 10 malformed/null cases safely mapped; nullable author/page sanitized")
 }
 @Test fun actualLookupHandlesHttpAndMalformedResponsesWithoutExternalService()= runBlocking {
  val reply=AtomicReference(500 to "not-json")
  val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
  server.createContext("/isbn") { x ->
   x.requestBody.use { it.readBytes() };val(code,body)=reply.get();val bytes=body.toByteArray()
   x.sendResponseHeaders(code,bytes.size.toLong());x.responseBody.use { it.write(bytes) }
  }
  server.start()
  try {
   val client=ShowApiClient("http://127.0.0.1:${server.address.port}/isbn")
   assertTrue(client.lookup("DIAGNOSTIC-NOT-A-REAL-KEY","9780306406157") is ShowApiResult.Failed)
   reply.set(200 to "{\"showapi_res_code\":0,\"showapi_res_body\":{\"ret_code\":0,\"data\":{\"title\":\"测试夹具\",\"author\":null,\"page\":null}}}")
   val result=client.lookup("DIAGNOSTIC-NOT-A-REAL-KEY","9780306406157");assertTrue(result is ShowApiResult.Ok)
   println("LOOKUP loopback HTTP500/invalid JSON and null author/page success both handled")
  } finally { server.stop(0) }
 }
}

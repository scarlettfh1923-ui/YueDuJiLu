package com.scarlett.yuedujilu.ui.common

import com.scarlett.yuedujilu.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scarlett.yuedujilu.ui.icons.AppIcons
import com.scarlett.yuedujilu.ui.theme.LocalPalette
import com.scarlett.yuedujilu.ui.theme.LocalRadii
import com.scarlett.yuedujilu.ui.theme.pressClickable

@Composable
fun SoftCard(
  modifier: Modifier = Modifier,
  radius: Int? = null,
  elevated: Boolean = false,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val p = LocalPalette.current
  val radii = LocalRadii.current
  val shape = UiShape.Regular
  val shadow = if (elevated) 2.dp else 0.dp
  Column(
    modifier = modifier
      .fillMaxWidth()
      .shadow(shadow, shape, ambientColor = p.shadow, spotColor = p.shadow)
      .clip(shape)
      .background(if (elevated) p.surfaceRaised else p.surface)
      .then(if (onClick != null) Modifier.pressClickable(onClick = onClick) else Modifier)
      .padding(20.dp),
    content = content,
  )
}

@Composable
fun IconButtonCircle(
  icon: ImageVector,
  contentDescription: String,
  modifier: Modifier = Modifier,
  size: Dp = 52.dp,
  emphasized: Boolean = false,
  onClick: () -> Unit,
) {
  val p = LocalPalette.current
  val shape = UiShape.Capsule
  Box(
    modifier = modifier
      .size(size.coerceAtLeast(48.dp))
      .porcelainSurface(shape,if(emphasized)p.primary else p.surfaceRaised,p.text,PorcelainLevel.CONTROL)
      .pressClickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription, tint = if (emphasized) p.onPrimary else p.text, modifier = Modifier.size(22.dp))
  }
}

@Composable
fun SearchField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String = "搜索书名、作者或 ISBN",
) {
  val p = LocalPalette.current
  val shape = UiShape.Capsule
  Row(
    modifier = modifier
      .defaultMinSize(minHeight = 52.dp)
      .porcelainSurface(shape,p.surfaceRaised,p.text,PorcelainLevel.CONTROL)
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(AppIcons.Search, contentDescription = null, tint = p.textTertiary, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(10.dp))
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      textStyle = TextStyle(color = p.text, fontSize = 15.sp),
      cursorBrush = SolidColor(p.primary),
      modifier = Modifier.weight(1f),
      decorationBox = { inner ->
        Box(contentAlignment = Alignment.CenterStart) {
          if (value.isEmpty()) Text(placeholder, color = p.textTertiary, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
          inner()
        }
      },
    )
  }
}

@Composable
fun FilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val p = LocalPalette.current
  val shape = UiShape.Capsule
  Box(
    modifier = Modifier
      .defaultMinSize(minHeight = 44.dp)
      .clip(shape)
      .background(if (selected) p.primary else p.surface)
      .pressClickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      label,
      color = if (selected) p.onPrimary else p.textSecondary,
      fontSize = 13.sp,
      fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
    )
  }
}

@Composable
fun PrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val p = LocalPalette.current
  val shape = UiShape.Regular
  Box(
    modifier = modifier
      .defaultMinSize(minHeight = 52.dp)
      .clip(shape)
      .background(if (enabled) p.primary else p.surfaceMuted)
      .pressClickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 18.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(text, color = if (enabled) p.onPrimary else p.textTertiary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
  }
}

@Composable
fun QuietButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val p = LocalPalette.current
  val shape = UiShape.Regular
  Box(
    modifier = modifier
      .defaultMinSize(minHeight = 52.dp)
      .clip(shape)
      .background(p.primarySoft)
      .pressClickable(onClick = onClick)
      .padding(horizontal = 18.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(text, color = p.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
  }
}

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
  val p = LocalPalette.current
  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(title, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, color = p.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 22.sp)
  }
}

@Composable
fun SettingsRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val p = LocalPalette.current
  Row(
    modifier = modifier
      .fillMaxWidth()
      .pressClickable(onClick = onClick)
      .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      Modifier.size(44.dp).clip(CircleShape).background(p.primarySoft),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, null, tint = p.primary, modifier = Modifier.size(18.dp))
    }
    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
      Text(title, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
      Text(subtitle, color = p.textSecondary, fontSize = 12.sp)
    }
    Icon(AppIcons.ChevronRight, null, tint = p.textTertiary, modifier = Modifier.size(18.dp))
  }
}

@Composable
fun FormField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  singleLine: Boolean = true,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  enabled: Boolean = true,
  secret: Boolean = false,
) {
  val p = LocalPalette.current
  Column(modifier) {
    Text(label, color = p.textSecondary, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    val shape = UiShape.Regular
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = singleLine,
      enabled = enabled,
      textStyle = TextStyle(color = p.text, fontSize = 16.sp),
      cursorBrush = SolidColor(p.primary),
      keyboardOptions = if(secret) KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Password,autoCorrectEnabled=false) else keyboardOptions,
      keyboardActions = keyboardActions,
      visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
      modifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(p.surface)
        .border(1.dp, p.stroke, shape)
        .defaultMinSize(minHeight = 56.dp)
        .padding(horizontal = 16.dp, vertical = 16.dp),
    )
  }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, trailing: @Composable RowScope.() -> Unit = {}) {
  val p = LocalPalette.current
  Row(
    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButtonCircle(AppIcons.ChevronLeft, "返回", onClick = onBack)
    Spacer(Modifier.width(12.dp))
    Text(title, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    trailing()
  }
}

@Composable
fun NoticeCard(text: String, modifier: Modifier = Modifier) {
  val p = LocalPalette.current
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(UiShape.Regular)
      .background(p.primarySoft)
      .padding(14.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(AppIcons.Info, null, tint = p.primary, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(10.dp))
    Text(text, color = p.text, fontSize = 13.sp, lineHeight = 20.sp)
  }
}


@Composable
fun StatusTabs(
  labels: List<Pair<String, Boolean>>, onSelect: (Int) -> Unit, modifier: Modifier = Modifier,
) {
  val p = LocalPalette.current
  Row(modifier.fillMaxWidth().clip(UiShape.Capsule).background(p.groupBase).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    labels.forEachIndexed { index, (label, selected) ->
      Box(Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
        .then(if(selected)Modifier.porcelainSurface(UiShape.Capsule,p.surfaceRaised,p.text,PorcelainLevel.CONTROL) else Modifier.clip(UiShape.Capsule))
        .pressClickable { onSelect(index) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(label, color=if(selected) p.primary else p.textSecondary, fontSize=13.sp,lineHeight=20.sp,
          fontWeight=if(selected) FontWeight.SemiBold else FontWeight.Normal, maxLines=1)
      }
    }
  }
}

@Composable
fun SourceSelector(
  label: String,
  options: List<Pair<String, Boolean>>,
  onSelect: (Int) -> Unit,
) {
  val p = LocalPalette.current
  var expanded by remember { mutableStateOf(false) }
  Box {
    Box(Modifier.defaultMinSize(minHeight=48.dp).pressClickable {expanded=true},contentAlignment=Alignment.Center) {
      Row(Modifier.porcelainSurface(UiShape.Capsule,p.surfaceRaised,p.text,PorcelainLevel.SUBTLE).padding(horizontal=10.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(label,color=p.textSecondary,fontSize=12.sp,maxLines=1)
        Spacer(Modifier.width(5.dp));Icon(AppIcons.ChevronDown,null,tint=p.textSecondary,modifier=Modifier.size(13.dp))
      }
    }
    androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = p.surface) {
      options.forEachIndexed { index, (text, selected) ->
        androidx.compose.material3.DropdownMenuItem(
          text = {
            Text(text, color = if (selected) p.primary else p.text, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
          },
          onClick = {
            expanded = false
            onSelect(index)
          },
        )
      }
    }
  }
}

package com.patialarental.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val Cream = Color(0xFFFAF5EC)
val Cream2 = Color(0xFFF0E7D6)
val White = Color(0xFFFFFDF9)
val Ink = Color(0xFF261B35)
val Ink2 = Color(0xFF6E6080)
val Ink3 = Color(0xFF9C8FAC)
val Amber = Color(0xFFEE9224)
val Ambers = Color(0xFFFDE9CE)
val Teal = Color(0xFF169E8A)
val Teals = Color(0xFFD6F2EC)
val Green = Color(0xFF18A060)
val Green1 = Color(0xFF3AC482)
val Greens = Color(0xFFD6F3E2)
val Lav = Color(0xFF8B76D6)
val Lavs = Color(0xFFE9E3F8)

@Composable
fun PrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Amber,
            onPrimary = Color.White,
            background = Cream,
            onBackground = Ink,
            surface = White,
            onSurface = Ink
        ),
        content = content
    )
}

@Composable
fun Clay(
    mod: Modifier = Modifier,
    pad: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var m = mod
        .shadow(6.dp, RoundedCornerShape(20.dp))
        .clip(RoundedCornerShape(20.dp))
        .background(Brush.verticalGradient(listOf(White, Color(0xFFFBF4E7))))
    if (onClick != null) m = m.clickable { onClick() }
    Column(m.padding(pad), content = content)
}

@Composable
fun Pill(
    text: String,
    bg: Color,
    fg: Color,
    mod: Modifier = Modifier,
    small: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    var m = mod.clip(RoundedCornerShape(50)).background(bg)
    if (onClick != null) m = m.clickable { onClick() }
    Box(m.padding(horizontal = if (small) 9.dp else 12.dp, vertical = if (small) 4.dp else 7.dp)) {
        Text(text, color = fg, fontSize = if (small) 11.sp else 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BigButton(
    text: String,
    mod: Modifier = Modifier,
    bg1: Color = Green1,
    bg2: Color = Green,
    onClick: () -> Unit
) {
    Box(
        mod
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(bg1, bg2)))
            .clickable { onClick() }
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun Cap(text: String, color: Color = Ink3) {
    Text(text.uppercase(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
}

@Composable
fun Avatar(letter: String, bg: Color, fg: Color, size: Dp = 42.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(50)).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (letter.isBlank()) "?" else letter.take(1).uppercase(),
            color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp
        )
    }
}

@Composable
fun ScreenTitle(title: String, sub: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(title, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(sub, color = Ink2, fontSize = 13.sp)
    }
}

fun timeStr(ts: Long): String {
    if (ts <= 0L) return ""
    val c = Calendar.getInstance()
    val now = Calendar.getInstance()
    c.timeInMillis = ts
    val sameDay = c.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    val fmt = if (sameDay) SimpleDateFormat("h:mm a", Locale.US) else SimpleDateFormat("d MMM", Locale.US)
    return fmt.format(Date(ts))
}

fun dateStr(ts: Long): String {
    if (ts <= 0L) return "Set nahi"
    return SimpleDateFormat("d MMM, h:mm a", Locale.US).format(Date(ts))
}

fun avatarBg(s: String): Color {
    val palette = listOf(Ambers, Lavs, Teals, Greens, Color(0xFFFBE0D6), Color(0xFFE6EEF8))
    val i = (s.hashCode() and 0x7FFFFFFF) % palette.size
    return palette[i]
}

fun avatarFg(s: String): Color {
    val palette = listOf(Amber, Lav, Teal, Green, Color(0xFFDE5E48), Color(0xFF406EBE))
    val i = (s.hashCode() and 0x7FFFFFFF) % palette.size
    return palette[i]
}

@Composable
fun LeadRow(lead: Lead, onClick: () -> Unit) {
    Clay(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(lead.name.ifBlank { lead.phone }, avatarBg(lead.phone), avatarFg(lead.phone))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    if (lead.name.isBlank()) lead.phone else lead.name,
                    color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${lead.kind}  ·  ${lead.area.ifBlank { "area nahi" }}",
                    color = Ink3, fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val (bg, fg) = when (lead.status) {
                    "Deal" -> Greens to Green
                    "Lost" -> Color(0xFFF3E7E4) to Color(0xFFB0564A)
                    "Visit" -> Teals to Teal
                    "Baat hui" -> Lavs to Lav
                    else -> Ambers to Amber
                }
                Pill(lead.status, bg, fg, small = true)
                if (lead.follow > 0) Text("⏰ " + timeStr(lead.follow), color = Ink3, fontSize = 10.sp)
            }
        }
    }
}

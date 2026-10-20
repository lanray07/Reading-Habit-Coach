package com.readinghabitcoach.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readinghabitcoach.app.data.Book
import java.util.Locale

@Composable fun PageTitle(title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp)); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}
@Composable fun SectionTitle(title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f)); trailing?.invoke()
    }
}
@Composable fun Paper(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))) {
        Column(Modifier.padding(22.dp), content = content)
    }
}
@Composable fun Primary(text: String, icon: ImageVector = Icons.Outlined.PlayArrow, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, enabled = enabled, modifier = Modifier.heightIn(min = 50.dp), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)) {
        Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Text(text)
    }
}
@Composable fun EmptyState(title: String, text: String, icon: ImageVector = Icons.Outlined.MenuBook, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp)); Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp)); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp)); action?.invoke()
    }
}
private val coverColours = listOf(Color(0xFF466354), Color(0xFF9B694D), Color(0xFF586A7D), Color(0xFF8A7759), Color(0xFF786078))
@Composable fun BookCover(book: Book, width: Int = 86, height: Int = 126) {
    Box(Modifier.size(width.dp, height.dp).clip(RoundedCornerShape(3.dp, 7.dp, 7.dp, 3.dp))
        .background(coverColours[Math.floorMod(book.colour, coverColours.size)])
        .semantics { contentDescription = "${book.title}, ${book.author}" }) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(Color.Black.copy(alpha = .16f)))
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Outlined.Spa, null, Modifier.size(17.dp), tint = Color(0xFFE7D4AB))
            Text(book.title, color = Color(0xFFFFF7E7), fontFamily = FontFamily.Serif, fontSize = if (width > 100) 20.sp else 14.sp,
                maxLines = 4, overflow = TextOverflow.Ellipsis, lineHeight = if (width > 100) 24.sp else 17.sp)
            Text(book.author, color = Color(0xFFEADAC3), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
@Composable fun Progress(value: Float, label: String) {
    Column(Modifier.semantics { contentDescription = label }) {
        LinearProgressIndicator(progress = { value.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(5.dp)),
            color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp)); Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun ChoiceRow(values: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value -> FilterChip(selected == value, { onSelect(value) }, label = { Text(value) }, modifier = Modifier.heightIn(min = 48.dp)) }
    }
}
fun durationLabel(ms: Long): String { val minutes = ms / 60_000; return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes} min" }
fun clockLabel(ms: Long): String { val s = ms / 1000; return String.format(Locale.getDefault(), "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60) }
@Composable fun Field(value: String, change: (String) -> Unit, label: String, modifier: Modifier = Modifier, single: Boolean = true) {
    OutlinedTextField(value, change, label = { Text(label) }, modifier = modifier.fillMaxWidth(), singleLine = single, shape = RoundedCornerShape(10.dp))
}

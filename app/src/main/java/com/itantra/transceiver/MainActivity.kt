package com.itantra.transceiver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.itantra.transceiver.ui.theme.ITantraTheme

private val RadioGreen = Color(0xFF71E6A5)
private val RadioPanel = Color(0xFF15201B)
class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); askPermissions(); enableEdgeToEdge(); setContent { ITantraTheme { RadioApp() } } }
 private fun askPermissions() { val p = buildList { add(Manifest.permission.RECORD_AUDIO); if (android.os.Build.VERSION.SDK_INT >= 31) { add(Manifest.permission.BLUETOOTH_CONNECT); add(Manifest.permission.BLUETOOTH_SCAN) }; if (android.os.Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) }.filter { ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED }; if (p.isNotEmpty()) ActivityCompat.requestPermissions(this, p.toTypedArray(), 44) }
}
private enum class RadioState(val label: String) { READY("READY"), LISTENING("LISTENING"), SENDING("SENDING"), ALERT("ALERT") }
private data class HistoryItem(val text: String, val language: String, val alert: Boolean)
@Composable private fun RadioApp() {
 var setup by remember { mutableStateOf(false) }; var state by remember { mutableStateOf(RadioState.READY) }; var language by remember { mutableStateOf("Hindi") }; var vox by remember { mutableStateOf(false) }; var alert by remember { mutableStateOf(false) }; var expanded by remember { mutableStateOf(false) }; val history = remember { mutableStateListOf<HistoryItem>() }
 Surface(color = Color(0xFF09110E), modifier = Modifier.fillMaxSize()) { if (setup) SetupScreen { setup = false } else Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 20.dp)) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text("iTANTRA", color = RadioGreen, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text("OFFLINE RADIO", color = Color.LightGray, style = MaterialTheme.typography.labelSmall) }; TextButton(onClick = { setup = true }) { Text("SETUP") } }
  Spacer(Modifier.height(14.dp)); StatusBar(state); Spacer(Modifier.height(12.dp))
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.weight(1f).border(1.dp, Color(0xFF355044), RoundedCornerShape(10.dp)).clickable { expanded = !expanded }.padding(12.dp)) { Text("$language ▾", color = Color.White) }; FilterChip(selected = vox, onClick = { vox = !vox }, label = { Text("VOX") }); FilterChip(selected = alert, onClick = { alert = !alert }, label = { Text("ALERT") }) }
  if (expanded) LanguagePicker { language = it; expanded = false }; Spacer(Modifier.weight(1f)); Text("LIVE TRANSCRIPTION", color = Color(0xFF9FB8AA), style = MaterialTheme.typography.labelSmall); Text(if (state == RadioState.READY) "Hold PTT to speak" else "Listening locally…", color = Color.White, modifier = Modifier.padding(top = 6.dp)); Spacer(Modifier.height(18.dp))
  Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.CenterHorizontally).size(212.dp).background(if (state == RadioState.ALERT) Color(0xFFB63333) else RadioGreen, CircleShape).clickable { if (state == RadioState.READY) state = RadioState.LISTENING else { state = RadioState.SENDING; history.add(0, HistoryItem("Voice note ready to transmit", language, alert)); state = RadioState.READY } }) { Text(if (state == RadioState.LISTENING) "RELEASE\nTO SEND" else "PUSH\nTO TALK", textAlign = TextAlign.Center, fontWeight = FontWeight.Black, color = Color(0xFF07110C), style = MaterialTheme.typography.headlineSmall) }
  Spacer(Modifier.height(16.dp)); Text("RECENT MESSAGES", color = Color(0xFF9FB8AA), style = MaterialTheme.typography.labelSmall); LazyColumn(Modifier.heightIn(max = 120.dp)) { items(history) { item -> Text("↑ ${item.text} · ${item.language}", color = if (item.alert) Color(0xFFFFAA77) else Color.White, modifier = Modifier.padding(vertical = 5.dp)) } }; Text("PEER: No peer selected  •  Wi‑Fi Direct preferred  •  Bluetooth fallback", color = Color(0xFF9FB8AA), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 8.dp))
 } }
}
@Composable private fun StatusBar(state: RadioState) = Surface(color = RadioPanel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).background(if (state == RadioState.ALERT) Color.Red else RadioGreen, CircleShape)); Spacer(Modifier.width(10.dp)); Text(state.label, color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("OFFLINE", color = RadioGreen, style = MaterialTheme.typography.labelSmall) } }
@Composable private fun LanguagePicker(choose: (String) -> Unit) { val names = listOf("Hindi", "Gujarati", "Marathi", "Kannada", "Malayalam", "Tamil", "Telugu", "Odia", "Bengali", "English"); Column(Modifier.fillMaxWidth().background(RadioPanel, RoundedCornerShape(10.dp)).padding(8.dp)) { names.chunked(2).forEach { row -> Row { row.forEach { name -> Text(name, color = Color.White, modifier = Modifier.weight(1f).clickable { choose(name) }.padding(8.dp)) } } } } }
@Composable private fun SetupScreen(onBack: () -> Unit) { Column(Modifier.fillMaxSize().padding(20.dp)) { TextButton(onClick = onBack) { Text("‹ RADIO") }; Text("SETUP", style = MaterialTheme.typography.headlineMedium, color = RadioGreen); Spacer(Modifier.height(18.dp)); listOf("Microphone permission" to "Required for PTT and VOX", "Language model packs" to "Install local ONNX packs from storage or a peer", "Peer pairing" to "Discover Wi‑Fi Direct or Bluetooth peers", "Alert playback" to "Check audio focus and notification-policy access").forEach { (title, detail) -> Surface(color = RadioPanel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) { Column(Modifier.padding(15.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(detail, color = Color.LightGray, style = MaterialTheme.typography.bodySmall) } } }; Spacer(Modifier.height(12.dp)); Text("Diagnostics: audio 16 kHz • protocol v1 • no active transport", color = Color(0xFF9FB8AA), style = MaterialTheme.typography.labelSmall) } }

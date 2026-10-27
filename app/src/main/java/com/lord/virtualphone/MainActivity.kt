package com.lord.virtualphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.lord.virtualphone.deviceprofile.DeviceProfile
import com.lord.virtualphone.deviceprofile.DeviceProfiles
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Root() }
    }
}

@Composable
fun Root() {
    var dark by remember { mutableStateOf(true) }
    var splash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1200); splash = false }
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            if (splash) Splash() else AppShell(dark) { dark = it }
        }
    }
}

@Composable
fun Splash() = Box(Modifier.fillMaxSize(), Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Lord Virtual Phone", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(dark: Boolean, onDark: (Boolean) -> Unit) {
    val nav = rememberNavController()
    var selected by remember { mutableStateOf(DeviceProfiles.all[0]) }
    val tabs = listOf(
        Triple("home", "Home", Icons.Default.Home),
        Triple("phone", "Phone", Icons.Default.Phone),
        Triple("tools", "Tools", Icons.Default.Build),
        Triple("settings", "Settings", Icons.Default.Settings)
    )
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        topBar = { TopAppBar(title = { Text("Lord Virtual Phone") }) },
        bottomBar = {
            NavigationBar {
                tabs.forEach { (r, label, icon) ->
                    NavigationBarItem(
                        selected = route == r, label = { Text(label) },
                        icon = { Icon(icon, label) },
                        onClick = { nav.navigate(r) { launchSingleTop = true; popUpTo("home") } })
                }
            }
        }
    ) { pad ->
        NavHost(nav, "home", Modifier.padding(pad)) {
            composable("home") { Home(selected) { selected = it } }
            composable("phone") { PhoneScreen(selected) }
            composable("tools") { Tools() }
            composable("settings") { SettingsScreen(dark, onDark) }
        }
    }
}

@Composable
fun Home(selected: DeviceProfile, onSelect: (DeviceProfile) -> Unit) {
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Selected virtual phone", style = MaterialTheme.typography.labelMedium)
                    Text("${selected.manufacturer} ${selected.model}", style = MaterialTheme.typography.titleLarge)
                    Text("Android ${selected.androidVersion} (API ${selected.apiLevel})")
                    Text("RAM ${selected.ramGb} GB • Storage ${selected.storageGb} GB")
                    Text("Root: Not available (runtime not integrated)")
                    Text("State: Stopped")
                }
            }
        }
        item { Text("Device Profiles", style = MaterialTheme.typography.titleMedium) }
        items(DeviceProfiles.all) { p ->
            Card(Modifier.fillMaxWidth().clickable { onSelect(p) }) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (p.id == selected.id) "✓ ${p.model}" else p.model,
                        style = MaterialTheme.typography.titleMedium)
                    Text("${p.manufacturer} • ${p.codename} • Android ${p.androidVersion}")
                    if (p.id == selected.id) {
                        Spacer(Modifier.height(8.dp))
                        listOf("CPU" to p.cpu, "Arch" to p.cpuArch, "Resolution" to "${p.resolution} @ ${p.dpi}dpi",
                            "GPU" to p.gpu, "Camera" to p.camera, "Audio" to p.audio,
                            "Bluetooth" to p.bluetooth, "Sensors" to p.sensors)
                            .forEach { (k, v) -> Text("$k: $v", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneScreen(p: DeviceProfile) {
    var msg by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(p.model, style = MaterialTheme.typography.headlineSmall)
        Text("State: Stopped")
        Spacer(Modifier.height(16.dp))
        Button(onClick = { msg = "Virtualization backend abhi integrate nahi hua (Phase 3). Guest boot nahi ho sakta." }) {
            Text("START VIRTUAL PHONE")
        }
    }
    if (msg != null) AlertDialog(onDismissRequest = { msg = null },
        confirmButton = { TextButton(onClick = { msg = null }) { Text("OK") } },
        title = { Text("Unavailable") }, text = { Text(msg!!) })
}

@Composable
fun Tools() {
    val items = listOf("File Transfer", "APK Manager", "Root", "Modules", "ROM Manager",
        "Kernel Manager", "Terminal", "Snapshots")
    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { name ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text("Not implemented yet — needs virtualization backend.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(dark: Boolean, onDark: (Boolean) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Dark theme"); Switch(dark, onDark)
        }
        Spacer(Modifier.height(16.dp))
        Text("Version 0.1.0 (MVP 0.1 — UI Prototype)", style = MaterialTheme.typography.bodySmall)
    }
}

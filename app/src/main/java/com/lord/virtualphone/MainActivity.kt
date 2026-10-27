package com.lord.virtualphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import com.lord.virtualphone.deviceprofile.DeviceProfile
import com.lord.virtualphone.deviceprofile.DeviceProfiles
import com.lord.virtualphone.logging.AppLog
import com.lord.virtualphone.virtualdevice.VirtualDeviceManager
import com.lord.virtualphone.virtualdevice.VmState
import kotlinx.coroutines.delay
import java.io.File

val BrandRed = Color(0xFFE53935)
val BgBlack = Color(0xFF0A0A0A)
val SurfaceDark = Color(0xFF151515)
val SurfaceVar = Color(0xFF211A1A)

private val DarkScheme = darkColorScheme(
    primary = BrandRed, onPrimary = Color.White,
    primaryContainer = Color(0xFF3A0D0D), onPrimaryContainer = Color.White,
    background = BgBlack, surface = SurfaceDark, surfaceVariant = SurfaceVar,
    onSurface = Color(0xFFF2F2F2), onSurfaceVariant = Color(0xFFBDBDBD)
)
private val LightScheme = lightColorScheme(primary = Color(0xFFC62828))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.init(this)
        AppLog.i("App started (v0.2.0)")
        setContent { Root() }
    }
}

@Composable
fun Root() {
    var dark by remember { mutableStateOf(true) }
    var splash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1400); splash = false }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (splash) Splash() else AppShell(dark) { dark = it }
        }
    }
}

@Composable
fun Splash() = Box(Modifier.fillMaxSize(), Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.logo), null,
            Modifier.size(180.dp).clip(RoundedCornerShape(28.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.height(20.dp))
        Text("Lord Virtual Phone", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator(color = BrandRed)
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
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.logo), null,
                            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(10.dp))
                        Text("Lord Virtual Phone", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { (r, label, icon) ->
                    NavigationBarItem(
                        selected = route == r, label = { Text(label) }, icon = { Icon(icon, label) },
                        onClick = { nav.navigate(r) { launchSingleTop = true; popUpTo("home") } })
                }
            }
        }
    ) { pad ->
        NavHost(nav, "home", Modifier.padding(pad)) {
            composable("home") { Home(selected) { if (it.enabled) selected = it } }
            composable("phone") { PhoneScreen(selected) }
            composable("tools") { Tools() }
            composable("settings") { SettingsScreen(dark, onDark) }
        }
    }
}

@Composable
fun Tile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StateChip(s: VmState) {
    val c = when (s) {
        VmState.STOPPED -> Color(0xFF616161)
        VmState.STARTING, VmState.STOPPING -> Color(0xFFF9A825)
        VmState.RUNNING -> Color(0xFF2E7D32)
        VmState.FAILED -> Color(0xFFC62828)
    }
    Surface(shape = RoundedCornerShape(50), color = c) {
        Text(s.name, Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
fun StatusRow(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(if (ok) Icons.Default.Check else Icons.Default.Close, null,
            tint = if (ok) Color(0xFF43A047) else BrandRed, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Home(selected: DeviceProfile, onSelect: (DeviceProfile) -> Unit) {
    val ctx = LocalContext.current
    val vm by VirtualDeviceManager.state.collectAsState()
    val st by VirtualDeviceManager.status.collectAsState()
    LaunchedEffect(selected.id) { VirtualDeviceManager.refresh(ctx, selected) }
    var target by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            if (target == "qemu")
                VirtualDeviceManager.importFile(ctx, uri, VirtualDeviceManager.qemuTarget(ctx), selected, true)
            else
                VirtualDeviceManager.importFile(ctx, uri, File(VirtualDeviceManager.guestDir(ctx, selected), target), selected, false)
        }
    }
    fun pick(t: String) { target = t; picker.launch(arrayOf("*/*")) }

    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)) {
        item {
            Card(Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.logo), null,
                        Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Lord Virtual Phone", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Isolated virtual Android phone", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Selected virtual phone", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${selected.manufacturer} ${selected.model}",
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        StateChip(vm)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tile("RAM", "${selected.ramGb} GB", Modifier.weight(1f))
                        Tile("Storage", "${selected.storageGb} GB", Modifier.weight(1f))
                        Tile("Screen", selected.resolution, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tile("Android", "${selected.androidVersion} (API ${selected.apiLevel})", Modifier.weight(1f))
                        Tile("Root", "Off", Modifier.weight(1f))
                        Tile("CPU", "ARM64", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Backend setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    StatusRow("QEMU binary (qemu-system-aarch64)", st.qemu)
                    StatusRow("Guest kernel", st.kernel)
                    StatusRow("Guest disk image (disk.img)", st.disk)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pick("qemu") }, Modifier.weight(1f)) { Text("QEMU", fontSize = 12.sp) }
                        OutlinedButton(onClick = { pick("kernel") }, Modifier.weight(1f)) { Text("Kernel", fontSize = 12.sp) }
                        OutlinedButton(onClick = { pick("disk.img") }, Modifier.weight(1f)) { Text("Disk", fontSize = 12.sp) }
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = { pick("initrd") }, Modifier.fillMaxWidth()) {
                        Text("Initrd (optional)", fontSize = 12.sp)
                    }
                }
            }
        }
        item { Text("Device Profiles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(DeviceProfiles.all) { p ->
            val isSel = p.id == selected.id
            val shape = RoundedCornerShape(16.dp)
            Card(
                Modifier.fillMaxWidth()
                    .then(if (isSel) Modifier.border(2.dp, BrandRed, shape) else Modifier)
                    .clickable(enabled = p.enabled) { onSelect(p) },
                shape = shape
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.model, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                            color = if (p.enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!p.enabled) Text("Coming later", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (isSel) Icon(Icons.Default.Check, null, tint = BrandRed)
                    }
                    Text("${p.manufacturer} • ${p.codename} • Android ${p.androidVersion}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (isSel) {
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
    val ctx = LocalContext.current
    val vm by VirtualDeviceManager.state.collectAsState()
    val logs by AppLog.lines.collectAsState()
    var err by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.width(110.dp).height(220.dp)
                .border(3.dp, BrandRed, RoundedCornerShape(18.dp)).padding(8.dp), Alignment.Center) {
                Text("Virtual display\n(Phase 4)", textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(p.model, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                StateChip(vm)
                Text("Android ${p.androidVersion} • ${p.resolution}", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { err = VirtualDeviceManager.start(ctx, p) },
                    enabled = vm == VmState.STOPPED || vm == VmState.FAILED,
                    modifier = Modifier.fillMaxWidth()) { Text("START VIRTUAL PHONE", fontSize = 12.sp) }
                OutlinedButton(onClick = { VirtualDeviceManager.stop() },
                    enabled = vm == VmState.RUNNING, modifier = Modifier.fillMaxWidth()) { Text("STOP") }
            }
        }
        Text("Console (QEMU serial + app log)", style = MaterialTheme.typography.labelLarge)
        Card(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(Modifier.padding(8.dp), state = listState) {
                items(logs) { Text(it, fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
            }
        }
    }
    if (err != null) AlertDialog(onDismissRequest = { err = null },
        confirmButton = { TextButton(onClick = { err = null }) { Text("OK") } },
        title = { Text("Cannot start") }, text = { Text(err!!) })
}

@Composable
fun Tools() {
    val items = listOf<Pair<String, ImageVector>>(
        "File Transfer" to Icons.Default.Share, "APK Manager" to Icons.Default.Add,
        "Root" to Icons.Default.Lock, "Modules" to Icons.Default.Build,
        "ROM Manager" to Icons.Default.Edit, "Kernel Manager" to Icons.Default.Info,
        "Terminal" to Icons.Default.List, "Snapshots" to Icons.Default.Refresh
    )
    LazyVerticalGrid(GridCells.Fixed(2), Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { (name, icon) ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Icon(icon, null, tint = BrandRed)
                    Spacer(Modifier.height(8.dp))
                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Needs backend", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(dark: Boolean, onDark: (Boolean) -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Display", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dark theme"); Switch(dark, onDark)
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Virtual hardware (default)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("CPU: cortex-a53 x4 (software emulation)\nRAM: 2048 MB\nNetwork: user-mode NAT",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Version 0.2.0 (Phase 3 backend PoC)", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { AppLog.clear() }) { Text("Clear console log") }
            }
        }
    }
}

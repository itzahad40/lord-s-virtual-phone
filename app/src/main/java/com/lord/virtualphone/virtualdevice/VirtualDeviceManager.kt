package com.lord.virtualphone.virtualdevice

import android.content.Context
import android.net.Uri
import com.lord.virtualphone.deviceprofile.DeviceProfile
import com.lord.virtualphone.logging.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import kotlin.concurrent.thread

enum class VmState { STOPPED, STARTING, RUNNING, STOPPING, FAILED }

data class BackendStatus(val qemu: Boolean = false, val kernel: Boolean = false, val disk: Boolean = false)

/**
 * Phase 3 backend (proof of concept): launches a real qemu-system-aarch64 process
 * and streams its serial console into AppLog. Nothing is faked: if a file is
 * missing or QEMU fails, the exact error is shown.
 *
 * Guest files live in filesDir/guests/<profileId>/ :
 *   kernel      (required)  aarch64 kernel Image
 *   disk.img    (required)  raw guest disk image
 *   initrd      (optional)
 *   cmdline.txt (optional)  kernel command line override
 */
object VirtualDeviceManager {
    val state = MutableStateFlow(VmState.STOPPED)
    val status = MutableStateFlow(BackendStatus())
    @Volatile private var process: Process? = null
    @Volatile private var stopRequested = false

    fun qemuTarget(ctx: Context) = File(ctx.filesDir, "qemu/bin/qemu-system-aarch64")

    fun qemuBinary(ctx: Context): File? {
        val lib = File(ctx.applicationInfo.nativeLibraryDir, "libqemu-system-aarch64.so")
        if (lib.exists()) return lib
        val local = qemuTarget(ctx)
        return if (local.exists()) local else null
    }

    fun guestDir(ctx: Context, p: DeviceProfile): File =
        File(ctx.filesDir, "guests/${p.id}").also { it.mkdirs() }

    fun refresh(ctx: Context, p: DeviceProfile) {
        val d = guestDir(ctx, p)
        status.value = BackendStatus(qemuBinary(ctx) != null, File(d, "kernel").exists(), File(d, "disk.img").exists())
    }

    fun importFile(ctx: Context, uri: Uri, dest: File, p: DeviceProfile, executable: Boolean) {
        thread(name = "import") {
            try {
                AppLog.i("Importing ${dest.name} ...")
                dest.parentFile?.mkdirs()
                ctx.contentResolver.openInputStream(uri)!!.use { input ->
                    dest.outputStream().use { out -> input.copyTo(out, 1 shl 16) }
                }
                if (executable) dest.setExecutable(true, false)
                AppLog.i("Imported ${dest.name} (${dest.length() / 1024} KB)")
            } catch (e: Exception) {
                AppLog.e("Import failed: ${e.message}")
            }
            refresh(ctx, p)
        }
    }

    /** Returns an error message if the guest cannot be started, null if start was launched. */
    fun start(ctx: Context, p: DeviceProfile): String? {
        if (state.value == VmState.STARTING || state.value == VmState.RUNNING) return "Already running."
        val bin = qemuBinary(ctx) ?: return "QEMU binary missing. Import qemu-system-aarch64 first."
        val dir = guestDir(ctx, p)
        val kernel = File(dir, "kernel")
        val disk = File(dir, "disk.img")
        if (!kernel.exists()) return "Guest kernel missing. Import a kernel first."
        if (!disk.exists()) return "Guest disk image missing. Import disk.img first."

        val cmd = mutableListOf(
            bin.absolutePath,
            "-machine", "virt", "-cpu", "cortex-a53", "-smp", "4", "-m", "2048",
            "-accel", "tcg,thread=multi",
            "-kernel", kernel.absolutePath
        )
        val initrd = File(dir, "initrd")
        if (initrd.exists()) cmd += listOf("-initrd", initrd.absolutePath)
        val append = File(dir, "cmdline.txt").takeIf { it.exists() }?.readText()?.trim()
            ?: "console=ttyAMA0 androidboot.selinux=permissive"
        cmd += listOf(
            "-append", append,
            "-drive", "if=none,file=${disk.absolutePath},format=raw,id=hd0",
            "-device", "virtio-blk-device,drive=hd0",
            "-netdev", "user,id=n0", "-device", "virtio-net-device,netdev=n0",
            "-nographic"
        )

        stopRequested = false
        state.value = VmState.STARTING
        thread(name = "qemu-${p.id}") {
            try {
                AppLog.i("Starting QEMU for ${p.model}")
                AppLog.i(cmd.joinToString(" "))
                val pb = ProcessBuilder(cmd).directory(dir).redirectErrorStream(true)
                val libs = File(ctx.filesDir, "qemu/lib")
                if (libs.exists()) pb.environment()["LD_LIBRARY_PATH"] = libs.absolutePath
                val proc = pb.start()
                process = proc
                state.value = VmState.RUNNING
                proc.inputStream.bufferedReader().forEachLine { AppLog.i(it) }
                val code = proc.waitFor()
                AppLog.i("QEMU exited with code $code")
                state.value = if (stopRequested || code == 0) VmState.STOPPED else VmState.FAILED
            } catch (e: Exception) {
                AppLog.e("QEMU failed to start: ${e.message}")
                state.value = VmState.FAILED
            } finally {
                process = null
            }
        }
        return null
    }

    fun stop() {
        val p = process ?: return
        stopRequested = true
        state.value = VmState.STOPPING
        AppLog.i("Stopping QEMU ...")
        p.destroy()
    }
}

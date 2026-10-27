package com.lord.virtualphone.logging

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

object AppLog {
    val lines = MutableStateFlow<List<String>>(emptyList())
    private var file: File? = null

    fun init(ctx: Context) {
        file = File(ctx.filesDir, "logs/app.log").also { it.parentFile?.mkdirs() }
    }

    @Synchronized
    private fun add(level: String, msg: String) {
        val line = "$level  $msg"
        Log.d("LordVP", line)
        lines.value = (lines.value + line).takeLast(400)
        try { file?.appendText(line + "\n") } catch (_: Exception) {}
    }

    fun i(msg: String) = add("I", msg)
    fun e(msg: String) = add("E", msg)
    fun clear() { lines.value = emptyList() }
}

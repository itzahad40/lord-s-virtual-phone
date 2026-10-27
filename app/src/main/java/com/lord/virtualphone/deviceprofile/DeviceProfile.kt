package com.lord.virtualphone.deviceprofile

data class DeviceProfile(
    val id: String,
    val manufacturer: String,
    val model: String,
    val codename: String,
    val androidVersion: String,
    val apiLevel: Int,
    val cpuArch: String,
    val cpu: String,
    val ramGb: Int,
    val storageGb: Int,
    val resolution: String,
    val dpi: Int,
    val gpu: String,
    val camera: String,
    val audio: String,
    val bluetooth: String,
    val sensors: String,
    val buildProps: Map<String, String>,
    val enabled: Boolean = true
)

// NOTE: Ye virtual device identity hai, physical host hardware nahi.
object DeviceProfiles {
    val all = listOf(
        DeviceProfile("redmi_y2", "Xiaomi", "Redmi Y2", "ysl", "8.1.0", 27,
            "arm64-v8a", "Snapdragon 625 (8x Cortex-A53)", 3, 32, "720x1440", 320,
            "Adreno 506", "12MP + 5MP rear, 16MP front", "Speaker + mic", "Bluetooth 4.2",
            "Accelerometer, Proximity, Light, Compass",
            mapOf("ro.product.model" to "Redmi Y2", "ro.product.device" to "ysl")),
        DeviceProfile("redmi_note5", "Xiaomi", "Redmi Note 5", "whyred", "7.1.2", 25,
            "arm64-v8a", "Snapdragon 636 (8x Kryo 260)", 4, 64, "1080x2160", 403,
            "Adreno 509", "12MP + 5MP rear, 13MP front", "Speaker + mic", "Bluetooth 5.0",
            "Accelerometer, Gyroscope, Proximity, Light, Compass",
            mapOf("ro.product.model" to "Redmi Note 5", "ro.product.device" to "whyred"), enabled = false),
        DeviceProfile("oneplus_3t", "OnePlus", "OnePlus 3T", "oneplus3", "6.0.1", 23,
            "arm64-v8a", "Snapdragon 821 (4x Kryo)", 6, 64, "1080x1920", 380,
            "Adreno 530", "16MP rear, 16MP front", "Speaker + mic", "Bluetooth 4.2",
            "Accelerometer, Gyroscope, Proximity, Light, Compass",
            mapOf("ro.product.model" to "ONEPLUS A3010", "ro.product.device" to "OnePlus3T"), enabled = false)
    )
}

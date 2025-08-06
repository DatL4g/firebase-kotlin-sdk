package dev.gitlive.firebase.remoteconfig

import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigSource
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigValue

public actual class FirebaseRemoteConfigValue internal constructor(private val apple: FIRRemoteConfigValue) {
    @ExperimentalUnsignedTypes
    public actual fun asByteArray(): ByteArray = apple.dataValue.toByteArray()

    public actual fun asBoolean(): Boolean = apple.boolValue
    public actual fun asDouble(): Double = apple.numberValue.doubleValue
    public actual fun asLong(): Long = apple.numberValue.longValue
    public actual fun asString(): String = apple.stringValue ?: ""
    public actual fun getSource(): ValueSource = when (apple.source) {
        FIRRemoteConfigSource.FIRRemoteConfigSourceStatic -> ValueSource.Static
        FIRRemoteConfigSource.FIRRemoteConfigSourceDefault -> ValueSource.Default
        FIRRemoteConfigSource.FIRRemoteConfigSourceRemote -> ValueSource.Remote
        else -> error("Unknown value source:${apple.source}")
    }
}

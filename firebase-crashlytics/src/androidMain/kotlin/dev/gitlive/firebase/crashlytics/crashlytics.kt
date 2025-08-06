package dev.gitlive.firebase.crashlytics

import com.google.firebase.FirebaseException
import com.google.firebase.crashlytics.CustomKeysAndValues.Builder
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp

public actual val Firebase.crashlytics: FirebaseCrashlytics get() =
    FirebaseCrashlytics(com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance())

public actual fun Firebase.crashlytics(app: FirebaseApp): FirebaseCrashlytics = FirebaseCrashlytics(app.android.get(com.google.firebase.crashlytics.FirebaseCrashlytics::class.java))

public actual class FirebaseCrashlytics internal constructor(internal val _android: com.google.firebase.crashlytics.FirebaseCrashlytics) {
    public val android: com.google.firebase.crashlytics.FirebaseCrashlytics
        get() = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()

    public actual fun recordException(exception: Throwable) {
        _android.recordException(exception)
    }
    public actual fun log(message: String) {
        _android.log(message)
    }
    public actual fun setUserId(userId: String) {
        _android.setUserId(userId)
    }
    public actual fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        _android.setCrashlyticsCollectionEnabled(enabled)
    }
    public actual fun sendUnsentReports() {
        _android.sendUnsentReports()
    }
    public actual fun deleteUnsentReports() {
        _android.deleteUnsentReports()
    }
    public actual fun didCrashOnPreviousExecution(): Boolean = _android.didCrashOnPreviousExecution()
    public actual fun setCustomKey(key: String, value: String) {
        _android.setCustomKey(key, value)
    }
    public actual fun setCustomKey(key: String, value: Boolean) {
        _android.setCustomKey(key, value)
    }
    public actual fun setCustomKey(key: String, value: Double) {
        _android.setCustomKey(key, value)
    }
    public actual fun setCustomKey(key: String, value: Float) {
        _android.setCustomKey(key, value)
    }
    public actual fun setCustomKey(key: String, value: Int) {
        _android.setCustomKey(key, value)
    }
    public actual fun setCustomKey(key: String, value: Long) {
        _android.setCustomKey(key, value)
    }
    public actual fun setCustomKeys(customKeys: Map<String, Any>) {
        _android.setCustomKeys(
            Builder().apply {
                customKeys.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Boolean -> putBoolean(key, value)
                        is Double -> putDouble(key, value)
                        is Float -> putFloat(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                    }
                }
            }.build(),
        )
    }
}

public actual open class FirebaseCrashlyticsException(message: String) : FirebaseException(message)

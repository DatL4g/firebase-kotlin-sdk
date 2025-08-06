package dev.gitlive.firebase.remoteconfig

import cocoapods.FirebaseRemoteConfig.FIRRemoteConfig
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigErrorDomain
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigErrorInternalError
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigErrorThrottled
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigFetchAndActivateStatus
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigFetchStatus
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigSettings
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigSource
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import dev.gitlive.firebase.app
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.toKotlinInstant
import platform.Foundation.NSError
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

public actual val Firebase.remoteConfig: FirebaseRemoteConfig
    get() = FirebaseRemoteConfig(FIRRemoteConfig.remoteConfig())

public actual fun Firebase.remoteConfig(app: FirebaseApp): FirebaseRemoteConfig = FirebaseRemoteConfig(
    FIRRemoteConfig.remoteConfigWithApp(Firebase.app.apple as objcnames.classes.FIRApp),
)

public actual class FirebaseRemoteConfig internal constructor(public val apple: FIRRemoteConfig) {
    @Suppress("UNCHECKED_CAST")
    public actual val all: Map<String, FirebaseRemoteConfigValue>
        get() {
            return listOf(
                FIRRemoteConfigSource.FIRRemoteConfigSourceStatic,
                FIRRemoteConfigSource.FIRRemoteConfigSourceRemote,
                FIRRemoteConfigSource.FIRRemoteConfigSourceDefault,
            ).map { source ->
                val keys = apple.allKeysFromSource(source) as List<String>
                keys.map { it to FirebaseRemoteConfigValue(apple.configValueForKey(it, source)) }
            }.flatten().toMap()
        }

    @OptIn(ExperimentalTime::class)
    public actual val info: FirebaseRemoteConfigInfo
        get() {
            return FirebaseRemoteConfigInfo(
                configSettings = apple.configSettings.asCommon(),
                fetchTime = apple.lastFetchTime?.toKotlinInstant()
                    ?.takeIf { it.toEpochMilliseconds() > 0 }
                    ?: Instant.fromEpochMilliseconds(-1),
                lastFetchStatus = apple.lastFetchStatus.asCommon(),
            )
        }

    public actual suspend fun activate(): Boolean = apple.awaitResult { activateWithCompletion(it) }

    public actual suspend fun ensureInitialized(): Unit = apple.await { ensureInitializedWithCompletionHandler(it) }

    public actual suspend fun fetch(minimumFetchInterval: Duration?) {
        if (minimumFetchInterval != null) {
            apple.awaitResult<FIRRemoteConfig, FIRRemoteConfigFetchStatus> {
                fetchWithExpirationDuration(minimumFetchInterval.toDouble(DurationUnit.SECONDS), it)
            }
        } else {
            apple.awaitResult { fetchWithCompletionHandler(it) }
        }
    }

    public actual suspend fun fetchAndActivate(): Boolean {
        val status: FIRRemoteConfigFetchAndActivateStatus = apple.awaitResult {
            fetchAndActivateWithCompletionHandler(it)
        }
        return status == FIRRemoteConfigFetchAndActivateStatus.FIRRemoteConfigFetchAndActivateStatusSuccessFetchedFromRemote
    }

    public actual fun getKeysByPrefix(prefix: String): Set<String> = all.keys.filter { it.startsWith(prefix) }.toSet()

    public actual fun getValue(key: String): FirebaseRemoteConfigValue = FirebaseRemoteConfigValue(apple.configValueForKey(key))

    public actual suspend fun reset() {
        // not implemented for iOS target
    }

    public actual suspend fun settings(init: FirebaseRemoteConfigSettings.() -> Unit) {
        val settings = FirebaseRemoteConfigSettings().apply(init)
        val iosSettings = FIRRemoteConfigSettings().apply {
            minimumFetchInterval = settings.minimumFetchInterval.toDouble(DurationUnit.SECONDS)
            fetchTimeout = settings.fetchTimeout.toDouble(DurationUnit.SECONDS)
        }
        apple.setConfigSettings(iosSettings)
    }

    public actual suspend fun setDefaults(vararg defaults: Pair<String, Any?>) {
        apple.setDefaults(defaults.toMap())
    }

    private fun FIRRemoteConfigSettings.asCommon(): FirebaseRemoteConfigSettings = FirebaseRemoteConfigSettings(
        fetchTimeout = fetchTimeout.seconds,
        minimumFetchInterval = minimumFetchInterval.seconds,
    )

    private fun FIRRemoteConfigFetchStatus.asCommon(): FetchStatus = when (this) {
        FIRRemoteConfigFetchStatus.FIRRemoteConfigFetchStatusSuccess -> FetchStatus.Success
        FIRRemoteConfigFetchStatus.FIRRemoteConfigFetchStatusNoFetchYet -> FetchStatus.NoFetchYet
        FIRRemoteConfigFetchStatus.FIRRemoteConfigFetchStatusFailure -> FetchStatus.Failure
        FIRRemoteConfigFetchStatus.FIRRemoteConfigFetchStatusThrottled -> FetchStatus.Throttled
        else -> FetchStatus.Failure
    }
}

private suspend inline fun <T, reified R> T.awaitResult(
    function: T.(callback: (R?, NSError?) -> Unit) -> Unit,
): R {
    val job = CompletableDeferred<R?>()
    function { result, error ->
        if (error == null) {
            job.complete(result)
        } else {
            job.completeExceptionally(error.toException())
        }
    }
    return job.await() as R
}

private suspend inline fun <T> T.await(function: T.(callback: (NSError?) -> Unit) -> Unit) {
    val job = CompletableDeferred<Unit>()
    function { error ->
        if (error == null) {
            job.complete(Unit)
        } else {
            job.completeExceptionally(error.toException())
        }
    }
    job.await()
}

private fun NSError.toException() = when (domain) {
    FIRRemoteConfigErrorDomain -> {
        when (code) {
            FIRRemoteConfigErrorThrottled -> FirebaseRemoteConfigFetchThrottledException(
                localizedDescription,
            )

            FIRRemoteConfigErrorInternalError -> FirebaseRemoteConfigServerException(
                localizedDescription,
            )

            else -> FirebaseRemoteConfigClientException(localizedDescription)
        }
    }

    else -> FirebaseException(localizedDescription)
}

public actual open class FirebaseRemoteConfigException(message: String) : FirebaseException(message)

public actual class FirebaseRemoteConfigClientException(message: String) : FirebaseRemoteConfigException(message)

public actual class FirebaseRemoteConfigFetchThrottledException(message: String) : FirebaseRemoteConfigException(message)

public actual class FirebaseRemoteConfigServerException(message: String) : FirebaseRemoteConfigException(message)

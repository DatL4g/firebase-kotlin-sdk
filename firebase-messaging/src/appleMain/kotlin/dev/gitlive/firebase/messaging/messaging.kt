package dev.gitlive.firebase.messaging

import cocoapods.FirebaseMessaging.FIRMessaging
import dev.gitlive.firebase.Firebase
import kotlinx.coroutines.CompletableDeferred
import platform.Foundation.NSError

public actual val Firebase.messaging: FirebaseMessaging
    get() = FirebaseMessaging(FIRMessaging.messaging())

public actual class FirebaseMessaging(public val apple: FIRMessaging) {
    public actual fun subscribeToTopic(topic: String) {
        apple.subscribeToTopic(topic)
    }

    public actual fun unsubscribeFromTopic(topic: String) {
        apple.unsubscribeFromTopic(topic)
    }

    public actual suspend fun getToken(): String = awaitResult { apple.tokenWithCompletion(it) }

    public actual suspend fun deleteToken() {
        await { apple.deleteTokenWithCompletion(it) }
    }
}

public suspend inline fun <T> T.await(function: T.(callback: (NSError?) -> Unit) -> Unit) {
    val job = CompletableDeferred<Unit>()
    function { error ->
        if (error == null) {
            job.complete(Unit)
        } else {
            job.completeExceptionally(Exception(error.toString()))
        }
    }
    job.await()
}

public suspend inline fun <T, reified R> T.awaitResult(function: T.(callback: (R?, NSError?) -> Unit) -> Unit): R {
    val job = CompletableDeferred<R?>()
    function { result, error ->
        if (error == null) {
            job.complete(result)
        } else {
            job.completeExceptionally(Exception(error.toString()))
        }
    }
    return job.await() as R
}

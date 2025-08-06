/*
 * Copyright (c) 2023 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.storage

import cocoapods.FirebaseStorage.FIRStorage
import cocoapods.FirebaseStorage.FIRStorageListResult
import cocoapods.FirebaseStorage.FIRStorageMetadata
import cocoapods.FirebaseStorage.FIRStorageReference
import cocoapods.FirebaseStorage.FIRStorageTaskStatusFailure
import cocoapods.FirebaseStorage.FIRStorageTaskStatusPause
import cocoapods.FirebaseStorage.FIRStorageTaskStatusProgress
import cocoapods.FirebaseStorage.FIRStorageTaskStatusResume
import cocoapods.FirebaseStorage.FIRStorageTaskStatusSuccess
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

public actual val Firebase.storage: FirebaseStorage get() =
    FirebaseStorage(FIRStorage.storage())

public actual fun Firebase.storage(url: String): FirebaseStorage = FirebaseStorage(
    FIRStorage.storageWithURL(url),
)

public actual fun Firebase.storage(app: FirebaseApp): FirebaseStorage = FirebaseStorage(
    FIRStorage.storageForApp(app.apple as objcnames.classes.FIRApp),
)

public actual fun Firebase.storage(app: FirebaseApp, url: String): FirebaseStorage = FirebaseStorage(
    FIRStorage.storageForApp(app.apple as objcnames.classes.FIRApp, url),
)

public actual class FirebaseStorage(public val apple: FIRStorage) {
    public actual val maxOperationRetryTime: Duration = apple.maxOperationRetryTime().seconds
    public actual val maxUploadRetryTime: Duration = apple.maxUploadRetryTime().seconds

    public actual fun setMaxOperationRetryTime(maxOperationRetryTime: Duration) {
        apple.setMaxOperationRetryTime(maxOperationRetryTime.toDouble(DurationUnit.SECONDS))
    }

    public actual fun setMaxUploadRetryTime(maxUploadRetryTime: Duration) {
        apple.setMaxUploadRetryTime(maxUploadRetryTime.toDouble(DurationUnit.SECONDS))
    }

    public actual fun useEmulator(host: String, port: Int) {
        apple.useEmulatorWithHost(host, port.toLong())
    }

    public actual val reference: StorageReference get() = StorageReference(apple.reference())

    public actual fun reference(location: String): StorageReference = StorageReference(apple.referenceWithPath(location))

    public actual fun getReferenceFromUrl(fullUrl: String): StorageReference = StorageReference(apple.referenceForURL(fullUrl))
}

public actual class StorageReference(public val apple: FIRStorageReference) {
    public actual val name: String get() = apple.name()
    public actual val path: String get() = apple.fullPath()
    public actual val bucket: String get() = apple.bucket()
    public actual val parent: StorageReference? get() = apple.parent()?.let { StorageReference(it) }
    public actual val root: StorageReference get() = StorageReference(apple.root())
    public actual val storage: FirebaseStorage get() = FirebaseStorage(apple.storage())

    public actual fun child(path: String): StorageReference = StorageReference(apple.child(path))

    public actual suspend fun getMetadata(): FirebaseStorageMetadata? = apple.awaitResult {
        metadataWithCompletion { metadata, error ->
            if (error == null) {
                it.invoke(metadata?.toFirebaseStorageMetadata(), null)
            } else {
                it.invoke(null, error)
            }
        }
    }

    public actual suspend fun delete(): Unit = await { apple.deleteWithCompletion(it) }

    public actual suspend fun getDownloadUrl(): String = apple.awaitResult {
        downloadURLWithCompletion(completion = it)
    }.absoluteString()!!

    public actual suspend fun listAll(): ListResult = awaitResult {
        apple.listAllWithCompletion { firStorageListResult, nsError ->
            it.invoke(firStorageListResult?.let { ListResult(it) }, nsError)
        }
    }

    public actual suspend fun putFile(file: File, metadata: FirebaseStorageMetadata?): Unit = apple.awaitResult { callback ->
        putFile(file.url, metadata?.toFIRMetadata(), callback)
    }.run {}

    public actual suspend fun putData(data: Data, metadata: FirebaseStorageMetadata?): Unit = apple.awaitResult { callback ->
        putData(data.data, metadata?.toFIRMetadata(), callback)
    }.run {}

    public actual fun putFileResumable(file: File, metadata: FirebaseStorageMetadata?): ProgressFlow {
        val ios = apple.putFile(file.url, metadata?.toFIRMetadata())

        val flow = callbackFlow {
            ios.observeStatus(FIRStorageTaskStatusProgress) {
                val progress = it!!.progress()!!
                trySendBlocking(Progress.Running(progress.completedUnitCount, progress.totalUnitCount))
            }
            ios.observeStatus(FIRStorageTaskStatusPause) {
                val progress = it!!.progress()!!
                trySendBlocking(Progress.Paused(progress.completedUnitCount, progress.totalUnitCount))
            }
            ios.observeStatus(FIRStorageTaskStatusResume) {
                val progress = it!!.progress()!!
                trySendBlocking(Progress.Running(progress.completedUnitCount, progress.totalUnitCount))
            }
            ios.observeStatus(FIRStorageTaskStatusSuccess) { close(FirebaseStorageException(it!!.error().toString())) }
            ios.observeStatus(FIRStorageTaskStatusFailure) {
                when (it!!.error()!!.code) {
                    /*FIRStorageErrorCodeCancelled = */
                    -13040L -> cancel(it.error()!!.localizedDescription)
                    else -> close(FirebaseStorageException(it.error().toString()))
                }
            }
            awaitClose { ios.removeAllObservers() }
        }

        return object : ProgressFlow {
            override suspend fun collect(collector: FlowCollector<Progress>) = collector.emitAll(flow)
            override fun pause() = ios.pause()
            override fun resume() = ios.resume()
            override fun cancel() = ios.cancel()
        }
    }
}

public actual class ListResult(public val apple: FIRStorageListResult) {
    public actual val prefixes: List<StorageReference> = apple.prefixes().map { StorageReference(it as FIRStorageReference) }
    public actual val items: List<StorageReference> = apple.items().map { StorageReference(it as FIRStorageReference) }
    public actual val pageToken: String? = apple.pageToken()
}

public actual class File(public val url: NSURL)

public actual class Data(public val data: NSData)

public actual class FirebaseStorageException(message: String) : FirebaseException(message)

internal suspend inline fun <T> T.await(function: T.(callback: (NSError?) -> Unit) -> Unit) {
    val job = CompletableDeferred<Unit>()
    function { error ->
        if (error == null) {
            job.complete(Unit)
        } else {
            job.completeExceptionally(FirebaseStorageException(error.toString()))
        }
    }
    job.await()
}

internal suspend inline fun <T, reified R> T.awaitResult(function: T.(callback: (R?, NSError?) -> Unit) -> Unit): R {
    val job = CompletableDeferred<R?>()
    function { result, error ->
        if (error == null) {
            job.complete(result)
        } else {
            job.completeExceptionally(FirebaseStorageException(error.toString()))
        }
    }
    return job.await() as R
}

internal fun FirebaseStorageMetadata.toFIRMetadata(): FIRStorageMetadata {
    val metadata = FIRStorageMetadata()
    val mappedMetadata: Map<Any?, String> = this.customMetadata.map {
        it.key to it.value
    }.toMap()
    metadata.setCustomMetadata(mappedMetadata)
    metadata.setCacheControl(this.cacheControl)
    metadata.setContentDisposition(this.contentDisposition)
    metadata.setContentEncoding(this.contentEncoding)
    metadata.setContentLanguage(this.contentLanguage)
    metadata.setContentType(this.contentType)
    return metadata
}

internal fun FIRStorageMetadata.toFirebaseStorageMetadata(): FirebaseStorageMetadata {
    val sdkMetadata = this
    return storageMetadata {
        md5Hash = sdkMetadata.md5Hash()
        cacheControl = sdkMetadata.cacheControl()
        contentDisposition = sdkMetadata.contentDisposition()
        contentEncoding = sdkMetadata.contentEncoding()
        contentLanguage = sdkMetadata.contentLanguage()
        contentType = sdkMetadata.contentType()
        sdkMetadata.customMetadata()?.forEach {
            setCustomMetadata(it.key.toString(), it.value.toString())
        }
    }
}

package dev.gitlive.firebase.firestore.internal

import dev.gitlive.firebase.firestore.EncodedFieldPath
import dev.gitlive.firebase.firestore.NativeDocumentReferenceType
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.await
import dev.gitlive.firebase.firestore.awaitResult
import dev.gitlive.firebase.firestore.toException
import dev.gitlive.firebase.internal.EncodedObject
import dev.gitlive.firebase.internal.apple
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

internal actual class NativeDocumentReference actual constructor(actual val nativeValue: NativeDocumentReferenceType) {

    actual fun snapshots(includeMetadataChanges: Boolean) = callbackFlow {
        val listener =
            apple.addSnapshotListenerWithIncludeMetadataChanges(includeMetadataChanges) { snapshot, error ->
                snapshot?.let { trySend(snapshot) }
                error?.let { close(error.toException()) }
            }
        awaitClose { listener.remove() }
    }

    val apple: NativeDocumentReferenceType by ::nativeValue

    actual val id: String
        get() = apple.documentID

    actual val path: String
        get() = apple.path

    actual val parent: NativeCollectionReferenceWrapper
        get() = NativeCollectionReferenceWrapper(apple.parent)

    actual fun collection(collectionPath: String) = apple.collectionWithPath(collectionPath)

    actual suspend fun get(source: Source) = awaitResult { apple.getDocumentWithSource(source.toIosSource(), it) }

    actual suspend fun setEncoded(encodedData: EncodedObject, setOptions: SetOptions) = await {
        when (setOptions) {
            is SetOptions.Merge -> apple.setData(encodedData.apple, true, it)
            is SetOptions.Overwrite -> apple.setData(encodedData.apple, false, it)
            is SetOptions.MergeFields -> apple.setData(encodedData.apple, setOptions.fields, it)
            is SetOptions.MergeFieldPaths -> apple.setData(
                encodedData.apple,
                setOptions.encodedFieldPaths,
                it,
            )
        }
    }

    actual suspend fun updateEncoded(encodedData: EncodedObject) = await {
        apple.updateData(encodedData.apple, it)
    }

    actual suspend fun updateEncodedFieldsAndValues(encodedFieldsAndValues: List<Pair<String, Any?>>) = await {
        apple.updateData(encodedFieldsAndValues.toMap(), it)
    }

    actual suspend fun updateEncodedFieldPathsAndValues(encodedFieldsAndValues: List<Pair<EncodedFieldPath, Any?>>) = await {
        apple.updateData(encodedFieldsAndValues.toMap(), it)
    }

    actual suspend fun delete() = await { apple.deleteDocumentWithCompletion(it) }

    actual val snapshots get() = callbackFlow {
        val listener = apple.addSnapshotListener { snapshot, error ->
            snapshot?.let { trySend(snapshot) }
            error?.let { close(error.toException()) }
        }
        awaitClose { listener.remove() }
    }

    override fun equals(other: Any?): Boolean = this === other || other is NativeDocumentReference && nativeValue == other.nativeValue
    override fun hashCode(): Int = nativeValue.hashCode()
    override fun toString(): String = nativeValue.toString()
}

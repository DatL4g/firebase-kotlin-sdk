package dev.gitlive.firebase.firestore.internal

import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.EncodedFieldPath
import dev.gitlive.firebase.firestore.NativeWriteBatch
import dev.gitlive.firebase.firestore.await
import dev.gitlive.firebase.firestore.apple
import dev.gitlive.firebase.internal.EncodedObject
import dev.gitlive.firebase.internal.apple

internal actual class NativeWriteBatchWrapper actual constructor(actual val native: NativeWriteBatch) {

    actual fun setEncoded(
        documentRef: DocumentReference,
        encodedData: EncodedObject,
        setOptions: SetOptions,
    ): NativeWriteBatchWrapper = when (setOptions) {
        is SetOptions.Merge -> native.setData(encodedData.apple, documentRef.apple, true)
        is SetOptions.Overwrite -> native.setData(encodedData.apple, documentRef.apple, false)
        is SetOptions.MergeFields -> native.setData(encodedData.apple, documentRef.apple, setOptions.fields)
        is SetOptions.MergeFieldPaths -> native.setData(encodedData.apple, documentRef.apple, setOptions.encodedFieldPaths)
    }.let { this }

    actual fun updateEncoded(documentRef: DocumentReference, encodedData: EncodedObject): NativeWriteBatchWrapper = native.updateData(encodedData.apple, documentRef.apple).let { this }

    actual fun updateEncodedFieldsAndValues(
        documentRef: DocumentReference,
        encodedFieldsAndValues: List<Pair<String, Any?>>,
    ): NativeWriteBatchWrapper = native.updateData(
        encodedFieldsAndValues.toMap(),
        documentRef.apple,
    ).let { this }

    actual fun updateEncodedFieldPathsAndValues(
        documentRef: DocumentReference,
        encodedFieldsAndValues: List<Pair<EncodedFieldPath, Any?>>,
    ): NativeWriteBatchWrapper = native.updateData(
        encodedFieldsAndValues.toMap(),
        documentRef.apple,
    ).let { this }

    actual fun delete(documentRef: DocumentReference) = native.deleteDocument(documentRef.apple).let { this }

    actual suspend fun commit() = await { native.commitWithCompletion(it) }
}

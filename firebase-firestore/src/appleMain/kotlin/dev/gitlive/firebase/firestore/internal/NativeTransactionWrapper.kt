package dev.gitlive.firebase.firestore.internal

import cocoapods.FirebaseFirestoreInternal.FIRTransaction
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.EncodedFieldPath
import dev.gitlive.firebase.firestore.apple
import dev.gitlive.firebase.internal.EncodedObject
import dev.gitlive.firebase.internal.apple

internal actual class NativeTransactionWrapper actual constructor(actual val native: FIRTransaction) {

    actual fun setEncoded(
        documentRef: DocumentReference,
        encodedData: EncodedObject,
        setOptions: SetOptions,
    ): NativeTransactionWrapper = when (setOptions) {
        is SetOptions.Merge -> native.setData(encodedData.apple, documentRef.apple, true)
        is SetOptions.Overwrite -> native.setData(encodedData.apple, documentRef.apple, false)
        is SetOptions.MergeFields -> native.setData(encodedData.apple, documentRef.apple, setOptions.fields)
        is SetOptions.MergeFieldPaths -> native.setData(encodedData.apple, documentRef.apple, setOptions.encodedFieldPaths)
    }.let { this }

    actual fun updateEncoded(documentRef: DocumentReference, encodedData: EncodedObject): NativeTransactionWrapper = native.updateData(encodedData.apple, documentRef.apple).let { this }

    actual fun updateEncodedFieldsAndValues(
        documentRef: DocumentReference,
        encodedFieldsAndValues: List<Pair<String, Any?>>,
    ): NativeTransactionWrapper = native.updateData(
        encodedFieldsAndValues.toMap(),
        documentRef.apple,
    ).let { this }

    actual fun updateEncodedFieldPathsAndValues(
        documentRef: DocumentReference,
        encodedFieldsAndValues: List<Pair<EncodedFieldPath, Any?>>,
    ): NativeTransactionWrapper = native.updateData(
        encodedFieldsAndValues.toMap(),
        documentRef.apple,
    ).let { this }

    actual fun delete(documentRef: DocumentReference) = native.deleteDocument(documentRef.apple).let { this }

    actual suspend fun get(documentRef: DocumentReference) = throwError { NativeDocumentSnapshotWrapper(native.getDocument(documentRef.apple, it)!!) }
}

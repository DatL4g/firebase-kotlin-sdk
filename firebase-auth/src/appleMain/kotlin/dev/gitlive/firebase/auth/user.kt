/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

import cocoapods.FirebaseAuth.FIRUserInfoProtocol
import cocoapods.FirebaseAuth.FIRUserMetadata

public actual class UserInfo(public val apple: FIRUserInfoProtocol) {
    public actual val displayName: String?
        get() = apple.displayName()
    public actual val email: String?
        get() = apple.email()
    public actual val phoneNumber: String?
        get() = apple.phoneNumber()
    public actual val photoURL: String?
        get() = apple.photoURL()?.absoluteString
    public actual val providerId: String
        get() = apple.providerID()
    public actual val uid: String
        get() = apple.uid()
}

public actual class UserMetaData(public val apple: FIRUserMetadata) {
    public actual val creationTime: Double?
        get() = apple.creationDate()?.timeIntervalSinceReferenceDate
    public actual val lastSignInTime: Double?
        get() = apple.lastSignInDate()?.timeIntervalSinceReferenceDate
}

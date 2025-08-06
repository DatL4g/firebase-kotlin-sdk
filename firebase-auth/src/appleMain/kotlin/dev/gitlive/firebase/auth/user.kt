/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

import cocoapods.FirebaseAuth.FIRAuthDataResult
import cocoapods.FirebaseAuth.FIRUser
import cocoapods.FirebaseAuth.FIRUserInfoProtocol
import cocoapods.FirebaseAuth.FIRUserMetadata
import platform.Foundation.NSURL

public actual class UserInfo(public val ios: FIRUserInfoProtocol) {
    public actual val displayName: String?
        get() = ios.displayName()
    public actual val email: String?
        get() = ios.email()
    public actual val phoneNumber: String?
        get() = ios.phoneNumber()
    public actual val photoURL: String?
        get() = ios.photoURL()?.absoluteString
    public actual val providerId: String
        get() = ios.providerID()
    public actual val uid: String
        get() = ios.uid()
}

public actual class UserMetaData(public val ios: FIRUserMetadata) {
    public actual val creationTime: Double?
        get() = ios.creationDate()?.timeIntervalSinceReferenceDate
    public actual val lastSignInTime: Double?
        get() = ios.lastSignInDate()?.timeIntervalSinceReferenceDate
}

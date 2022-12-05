/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.oci.constant

const val BLOB_UNKNOWN_CODE = "BLOB_UNKNOWN"
const val BLOB_UNKNOWN_MESSAGE = "blob unknown to registry"
const val BLOB_UNKNOWN_DESCRIPTION = "This error MAY be returned when a blob is unknown to the " +
    "registry in a specified repository. This can be returned with a standard get or if a manifest " +
    "references an unknown layer during upload."

const val BLOB_UPLOAD_INVALID_CODE = "BLOB_UPLOAD_INVALID"
const val BLOB_UPLOAD_INVALID_MESSAGE = "blob upload invalid"
const val BLOB_UPLOAD_INVALID_DESCRIPTION = "The blob upload encountered an error and can no longer proceed."

const val BLOB_UPLOAD_UNKNOWN_CODE = "BLOB_UPLOAD_UNKNOWN"
const val BLOB_UPLOAD_UNKNOWN_MESSAGE = "blob upload unknown to registry"
const val BLOB_UPLOAD_UNKNOWN_DESCRIPTION = "If a blob upload has been cancelled or was never started, " +
    "this error code MAY be returned."

const val DIGEST_INVALID_CODE = "DIGEST_INVALID"
const val DIGEST_INVALID_MESSAGE = "provided digest did not match uploaded content"
const val DIGEST_INVALID_DESCRIPTION = "When a blob is uploaded, the registry will check " +
    "that the content matches the digest provided by the client. " +
    "The error MAY include a detail structure with the key \"digest\", including the invalid digest string. " +
    "This error MAY also be returned when a manifest includes an invalid layer digest."

const val MANIFEST_BLOB_UNKNOWN_CODE = "MANIFEST_BLOB_UNKNOWN"
const val MANIFEST_BLOB_UNKNOWN_MESSAGE = "blob unknown to registry"
const val MANIFEST_BLOB_UNKNOWN_DESCRIPTION = "This error MAY be returned when a manifest blob " +
    "is unknown to the registry."

const val MANIFEST_INVALID_CODE = "MANIFEST_INVALID"
const val MANIFEST_INVALID_MESSAGE = "manifest invalid"
const val MANIFEST_INVALID_DESCRIPTION = "During upload, manifests undergo several checks ensuring validity. " +
    "If those checks fail, this error MAY be returned, unless a more specific error is included. " +
    "The detail will contain information the failed validation."

const val MANIFEST_UNKNOWN_CODE = "MANIFEST_UNKNOWN"
const val MANIFEST_UNKNOWN_MESSAGE = "manifest unknown"
const val MANIFEST_UNKNOWN_DESCRIPTION = "This error is returned when the manifest, " +
    "identified by name and tag is unknown to the repository."

const val MANIFEST_UNVERIFIED_CODE = "MANIFEST_UNVERIFIED"
const val MANIFEST_UNVERIFIED_MESSAGE = "manifest failed signature verification"
const val MANIFEST_UNVERIFIED_DESCRIPTION = "During manifest upload, if the manifest fails signature verification, " +
    "this error will be returned."

const val NAME_INVALID_CODE = "NAME_INVALID"
const val NAME_INVALID_MESSAGE = "invalid repository name"
const val NAME_INVALID_DESCRIPTION = "Invalid repository name encountered either during " +
    "manifest validation or any API operation."

const val NAME_UNKNOWN_CODE = "NAME_UNKNOWN"
const val NAME_UNKNOWN_MESSAGE = "repository name not known to registry"
const val NAME_UNKNOWN_DESCRIPTION = "This is returned if the name used during an operation is unknown to the registry."

const val SIZE_INVALID_CODE = "SIZE_INVALID"
const val SIZE_INVALID_MESSAGE = "provided length did not match content length"
const val SIZE_INVALID_DESCRIPTION = "When a layer is uploaded, " +
    "the provided size will be checked against the uploaded content. " +
    "If they do not match, this error will be returned."

const val TAG_INVALID_CODE = "TAG_INVALID"
const val TAG_INVALID_MESSAGE = "manifest tag did not match URI"
const val TAG_INVALID_DESCRIPTION = "During a manifest upload," +
    " if the tag in the manifest does not match the uri tag, " +
    "this error will be returned."

const val UNAUTHORIZED_CODE = "UNAUTHORIZED"
const val UNAUTHORIZED_MESSAGE = "authentication required"
const val UNAUTHORIZED_DESCRIPTION = "The access controller was unable to authenticate the client. " +
    "Often this will be accompanied by a Www-Authenticate HTTP response header indicating how to authenticate."

const val DENIED_CODE = "DENIED"
const val DENIED_MESSAGE = "requested access to the resource is denied"
const val DENIED_DESCRIPTION = "The access controller denied access for the operation on a resource."

const val UNSUPPORTED_CODE = "UNSUPPORTED"
const val UNSUPPORTED_MESSAGE = "The operation is unsupported."
const val UNSUPPORTED_DESCRIPTION = "The operation was unsupported due to a missing implementation or " +
    "invalid set of parameters."

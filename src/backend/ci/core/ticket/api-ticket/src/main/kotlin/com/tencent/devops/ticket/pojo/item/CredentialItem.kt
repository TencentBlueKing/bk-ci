/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.ticket.pojo.item

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.ticket.pojo.enums.CredentialType
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Base64

@Schema(title = "凭证值模型模型-多态基类")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(value = UserPassCredentialItem::class, name = UserPassCredentialItem.classType),
    JsonSubTypes.Type(value = TokenUserPassCredentialItem::class, name = TokenUserPassCredentialItem.classType),
    JsonSubTypes.Type(
        value = TokenSshPrivateKeyCredentialItem::class, name = TokenSshPrivateKeyCredentialItem.classType
    ),
    JsonSubTypes.Type(value = SshPrivateKeyCredentialItem::class, name = SshPrivateKeyCredentialItem.classType),
    JsonSubTypes.Type(value = PasswordCredentialItem::class, name = PasswordCredentialItem.classType),
    JsonSubTypes.Type(value = OauthTokenCredentialItem::class, name = OauthTokenCredentialItem.classType),
    JsonSubTypes.Type(value = AccessTokenCredentialItem::class, name = AccessTokenCredentialItem.classType)
)
interface CredentialItem {

    fun decode(publicKey: String, privateKey: ByteArray): CredentialItem

    fun decode(encode: String, publicKey: String, privateKey: ByteArray): String {
        val decoder = Base64.getDecoder()
        return String(DHUtil.decrypt(decoder.decode(encode), decoder.decode(publicKey), privateKey))
    }

    @JsonIgnore
    fun getCredentialType(): CredentialType
}

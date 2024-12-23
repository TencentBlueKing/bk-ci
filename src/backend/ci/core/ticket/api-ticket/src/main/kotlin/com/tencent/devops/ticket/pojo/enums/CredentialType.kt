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

package com.tencent.devops.ticket.pojo.enums

enum class CredentialType {
    PASSWORD, // v1 = password, v2=v3=v4=null
    ACCESSTOKEN, // v1 = access_token
    OAUTHTOKEN, // v1 = oauth_token
    USERNAME_PASSWORD, // v1 = username, v2 = password, other = null
    SECRETKEY, // v1 = secretKey, other = null
    APPID_SECRETKEY, // v1 = appId, v2 = secretKey, other = null
    SSH_PRIVATEKEY, // v1 = privateKey, v2=passphrase?
    TOKEN_SSH_PRIVATEKEY, // v1 = token, v2 = privateKey, v3=passphrase?
    TOKEN_USERNAME_PASSWORD, // v1 = token, v2 = username, v3=password
    COS_APPID_SECRETID_SECRETKEY_REGION, // v1 = cosappId, v2 = secretId, v3 = secretKey, v4 = region
    MULTI_LINE_PASSWORD; // 密码中有换行符 v1 = password, v2=v3=v4=null

    companion object {
        fun getKeyMap(credentialType: String): Map<String, String> {
            val keyMap = mutableMapOf<String, String>()
            when (credentialType) {
                PASSWORD.name -> keyMap["v1"] = "password"
                ACCESSTOKEN.name -> keyMap["v1"] = "access_token"
                OAUTHTOKEN.name -> keyMap["v1"] = "oauth_token"
                USERNAME_PASSWORD.name -> {
                    keyMap["v1"] = "username"
                    keyMap["v2"] = "password"
                }
                SECRETKEY.name -> keyMap["v1"] = "secretKey"
                APPID_SECRETKEY.name -> {
                    keyMap["v1"] = "appId"
                    keyMap["v2"] = "secretKey"
                }
                SSH_PRIVATEKEY.name -> {
                    keyMap["v1"] = "privateKey"
                    keyMap["v2"] = "passphrase"
                }
                TOKEN_SSH_PRIVATEKEY.name -> {
                    keyMap["v1"] = "token"
                    keyMap["v2"] = "privateKey"
                    keyMap["v3"] = "passphrase"
                }
                TOKEN_USERNAME_PASSWORD.name -> {
                    keyMap["v1"] = "token"
                    keyMap["v2"] = "username"
                    keyMap["v3"] = "password"
                }
                COS_APPID_SECRETID_SECRETKEY_REGION.name -> {
                    keyMap["v1"] = "cosappId"
                    keyMap["v2"] = "secretId"
                    keyMap["v3"] = "secretKey"
                    keyMap["v4"] = "region"
                }
                MULTI_LINE_PASSWORD.name -> keyMap["v1"] = "password"
            }
            return keyMap
        }
    }
}

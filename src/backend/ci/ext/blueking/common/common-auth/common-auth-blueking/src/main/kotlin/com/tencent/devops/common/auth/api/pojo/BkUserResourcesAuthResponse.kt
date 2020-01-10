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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.auth.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class BkUserResourcesAuthResponse(
    val code: Int, // 0
    val data: List<Data>?,
    val message: String?,
    @JsonProperty("request_id")
    val requestId: String?, // 20919aed2ed8441e872b634a15d809fd
    val result: Boolean // true
) {
    data class Data(
        @JsonProperty("action_id")
        val actionId: String, // list

        // 若resource_ids数组中出现空数组，表明对该action下所有资源都拥有权限，
        // 权限中心返回体中使用了 空数组 [] 替代料存入的 “*”
        @JsonProperty("resource_ids")
        val resourceIds: List<List<Map<String, String>?>?>,
        @JsonProperty("resource_type")
        val resourceType: String // credential
    )
}
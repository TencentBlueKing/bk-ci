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

package com.tencent.devops.dispatch.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * {
 *   "code": 0,
 *   "data": {
 *     "cluster_id": "BCS-K8S-15018",
 *     "created_at": "2018-06-26T20:35:03.144638659+08:00",
 *     "creator": "pipeline",
 *     "description": "",
 *     "env_type": "dev",
 *     "id": 118,
 *     "name": "test-by-api",
 *     "project_id": "b3b58d228f244c13b83bef3af882155c",
 *     "status": "",
 *     "updated_at": "2018-06-26T20:35:03.144638659+08:00"
 *   },
 *   "message": "注册Namespace成功",
 *   "request_id": "73466ffa-fb7d-4306-8265-f1655afbde8d",
 *   "result": true
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)@Suppress("ALL")
data class BCSCreateNamespaceResponse(
    val code: Int,
    val message: String,
    val request_id: String,
    val result: Boolean,
    val data: BCSCreateNamespaceData
)

@JsonIgnoreProperties(ignoreUnknown = true)@Suppress("ALL")
data class BCSCreateNamespaceData(
    val cluster_id: String,
    val created_at: String,
    val creator: String,
    val env_type: String,
    val id: Long,
    val name: String,
    val project_id: String
)

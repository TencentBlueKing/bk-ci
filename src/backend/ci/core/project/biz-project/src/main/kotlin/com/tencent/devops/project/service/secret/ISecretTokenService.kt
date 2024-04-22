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

package com.tencent.devops.project.service.secret

import com.tencent.devops.project.pojo.ProjectCallbackData
import com.tencent.devops.project.pojo.SecretRequestParam
import java.lang.Exception

interface ISecretTokenService<T> {
    /**
     * 获取URL/请求头/URL参数
     * @param userId 用户ID
     * @param projectId 蓝盾项目ID
     * @param secretParam 回调参数
     */
    fun getSecretRequestParam(
        userId: String,
        projectId: String,
        secretParam: T
    ): SecretRequestParam

    /**
     * 获取请求体内容
     * @param secretParam 回调参数
     * @param projectCallbackData 项目回调参数[更新/创建/禁用事件信息]
     */
    fun getRequestBody(secretParam: T, projectCallbackData: ProjectCallbackData): String

    /**
     * 获取请求类型
     */
    fun getRequestMethod(secretParam: T, projectCallbackData: ProjectCallbackData) = "POST"

    /**
     * 请求失败回调动作
     */
    fun requestFail(exception: Exception) = Unit

    /**
     * 请求成功回调动作
     */
    fun requestSuccess(responseBody: String) = Unit
}
/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.security.http.core

import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.http.credentials.HttpAuthCredentials
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * http请求认证处理器
 */
interface HttpAuthHandler {

    /**
     * 登录endpoint，表示该handler用于处理登录请求
     * 支持ant路径匹配规则
     */
    fun getLoginEndpoint(): String? = null

    /**
     * 登录endpoint的请求方法，表示该handler用于处理登录请求
     * 默认返回null, 表示所有请求方法
     */
    fun getLoginMethod(): String? = null

    /**
     * 提取认证身份信息
     */
    @Throws(AuthenticationException::class)
    fun extractAuthCredentials(request: HttpServletRequest): HttpAuthCredentials

    /**
     * 进行认证
     * 认证成功返回用户id，失败则抛AuthenticationException异常
     */
    @Throws(AuthenticationException::class)
    fun onAuthenticate(request: HttpServletRequest, authCredentials: HttpAuthCredentials): String

    /**
     * 认证失败回调
     * 可以根据各自依赖源的协议返回不同的数据格式
     */
    fun onAuthenticateFailed(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authenticationException: AuthenticationException
    ) {
        // 默认向上抛异常
        throw authenticationException
    }

    /**
     * 认证成功回调
     */
    fun onAuthenticateSuccess(request: HttpServletRequest, response: HttpServletResponse, userId: String) { }
}

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

package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.repository.pojo.proxy.ProxyChannelCreateRequest
import com.tencent.bkrepo.repository.pojo.proxy.ProxyChannelInfo

/**
 * 代理源服务接口
 */
interface ProxyChannelService {

    /**
     * 根据[id]查询代理源信息
     */
    fun findById(id: String): ProxyChannelInfo?

    /**
     * 根据[request]创建代理源
     */
    fun createProxy(userId: String, request: ProxyChannelCreateRequest)

    /**
     * 判断id为[id]类型为[repoType]的代理源是否存在
     */
    fun checkExistById(id: String, repoType: RepositoryType): Boolean

    /**
     * 判断名称为[name]类型为[repoType]的代理源是否存在
     */
    fun checkExistByName(name: String, repoType: RepositoryType): Boolean

    /**
     * 判断url为[url]类型为[repoType]的代理源是否存在
     */
    fun checkExistByUrl(url: String, repoType: RepositoryType): Boolean

    /**
     * 列表查询公有源
     */
    fun listPublicChannel(repoType: RepositoryType): List<ProxyChannelInfo>
}

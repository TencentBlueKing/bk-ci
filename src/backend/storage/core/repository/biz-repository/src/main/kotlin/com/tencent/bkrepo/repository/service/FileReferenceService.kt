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

import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.model.TRepository

/**
 * 文件引用服务接口
 */
interface FileReferenceService {
    /**
     * 增加文件sha256的引用数量
     *
     * sha256为[node]的属性，[repository]不为空则取credentialsKey属性，否则从[node]中取出repoName后从数据库查询
     * 增加引用成功则返回`true`; 如果[node]为目录，返回`false`
     */
    fun increment(node: TNode, repository: TRepository? = null): Boolean

    /**
     * 减少sha256的引用数量
     *
     * sha256为[node]的属性，[repository]不为空则取credentialsKey属性，否则从[node]中取出repoName后从数据库查询
     * 减少引用成功则返回`true`; 如果[node]为目录，返回`false`; 如果当前sha256的引用已经为0，返回`false`
     */
    fun decrement(node: TNode, repository: TRepository? = null): Boolean

    /**
     * 增加文件[sha256]在存储实例[credentialsKey]上的引用数量
     *
     * [credentialsKey]为`null`则使用默认的存储实例
     * 增加引用成功则返回`true`
     */
    fun increment(sha256: String, credentialsKey: String?): Boolean

    /**
     * 减少文件[sha256]在存储实例[credentialsKey]上的文件数量
     *
     * [credentialsKey]为`null`则使用默认的存储实例
     * 减少引用成功则返回`true`，如果当前[sha256]的引用已经为0，返回`false`
     */
    fun decrement(sha256: String, credentialsKey: String?): Boolean

    /**
     * 统计文件[sha256]在存储实例[credentialsKey]上的文件引用数量
     *
     * [credentialsKey]为`null`则使用默认的存储实例
     */
    fun count(sha256: String, credentialsKey: String?): Long
}

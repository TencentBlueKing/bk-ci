/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.service.node

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateRequest

/**
 * 节点CRUD基本操作接口
 */
interface NodeBaseOperation {

    /**
     * 查询节点详情
     */
    fun getNodeDetail(artifact: ArtifactInfo, repoType: String? = null): NodeDetail?

    /**
     * 列表查询节点
     */
    fun listNode(artifact: ArtifactInfo, option: NodeListOption): List<NodeInfo>

    /**
     * 分页查询节点
     */
    fun listNodePage(artifact: ArtifactInfo, option: NodeListOption): Page<NodeInfo>

    /**
     * 判断节点是否存在
     */
    fun checkExist(artifact: ArtifactInfo): Boolean

    /**
     * 判断节点列表是否存在
     */
    fun listExistFullPath(projectId: String, repoName: String, fullPathList: List<String>): List<String>

    /**
     * 创建节点，返回节点详情
     */
    fun createNode(createRequest: NodeCreateRequest): NodeDetail

    /**
     * 更新节点
     */
    fun updateNode(updateRequest: NodeUpdateRequest)
}

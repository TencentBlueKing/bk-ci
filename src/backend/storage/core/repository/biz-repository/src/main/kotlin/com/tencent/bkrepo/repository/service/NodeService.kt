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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateRequest
import com.tencent.bkrepo.repository.util.NodeUtils.ROOT_PATH

/**
 * 节点服务
 */
interface NodeService {
    /**
     * 查询节点详情
     */
    fun detail(projectId: String, repoName: String, fullPath: String, repoType: String? = null): NodeDetail?

    /**
     * 计算文件或者文件夹大小
     */
    fun computeSize(projectId: String, repoName: String, fullPath: String): NodeSizeInfo

    /**
     * 查询文件节点数量
     */
    fun countFileNode(projectId: String, repoName: String, path: String = ROOT_PATH): Long

    /**
     * 列表查询节点
     */
    fun list(
        projectId: String,
        repoName: String,
        path: String,
        includeFolder: Boolean = true,
        includeMetadata: Boolean = false,
        deep: Boolean = false
    ): List<NodeInfo>

    /**
     * 分页查询节点
     */
    fun page(
        projectId: String,
        repoName: String,
        path: String,
        page: Int,
        size: Int,
        includeFolder: Boolean = true,
        includeMetadata: Boolean = false,
        deep: Boolean = false
    ): Page<NodeInfo>

    /**
     * 判断节点是否存在
     */
    fun exist(projectId: String, repoName: String, fullPath: String): Boolean

    /**
     * 判断节点列表是否存在
     */
    fun listExistFullPath(projectId: String, repoName: String, fullPathList: List<String>): List<String>

    /**
     * 创建节点，返回id
     */
    fun create(createRequest: NodeCreateRequest): NodeDetail

    /**
     * 创建根节点
     */
    fun createRootNode(projectId: String, repoName: String, operator: String)

    /**
     * 重命名文件或者文件夹
     * 重命名过程中出现错误则抛异常，剩下的文件不会再移动
     * 遇到同名文件或者文件夹直接抛异常
     */
    fun rename(renameRequest: NodeRenameRequest)

    /**
     * 更新节点
     */
    fun update(updateRequest: NodeUpdateRequest)

    /**
     * 移动文件或者文件夹
     * 采用fast-failed模式，移动过程中出现错误则抛异常，剩下的文件不会再移动
     * 行为类似linux mv命令
     * mv 文件名 文件名	将源文件名改为目标文件名
     * mv 文件名 目录名	将文件移动到目标目录
     * mv 目录名 目录名	目标目录已存在，将源目录（目录本身及子文件）移动到目标目录；目标目录不存在则改名
     * mv 目录名 文件名	出错
     */
    fun move(moveRequest: NodeMoveRequest)

    /**
     * 拷贝文件或者文件夹
     * 采用fast-failed模式，拷贝过程中出现错误则抛异常，剩下的文件不会再拷贝
     * 行为类似linux cp命令
     * cp 文件名 文件名	将源文件拷贝到目标文件
     * cp 文件名 目录名	将文件移动到目标目录下
     * cp 目录名 目录名	cp 目录名 目录名	目标目录已存在，将源目录（目录本身及子文件）拷贝到目标目录；目标目录不存在则将源目录下文件拷贝到目标目录
     * cp 目录名 文件名	出错
     */
    fun copy(copyRequest: NodeCopyRequest)

    /**
     * 删除指定节点, 逻辑删除
     */
    fun delete(deleteRequest: NodeDeleteRequest)

    /**
     * 根据全路径删除文件或者目录
     */
    fun deleteByPath(projectId: String, repoName: String, fullPath: String, operator: String, soft: Boolean = true)
}

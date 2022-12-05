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

package com.tencent.bkrepo.replication.service

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.replication.pojo.cluster.ClusterListOption
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeCreateRequest
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeInfo
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeName
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeStatusUpdateRequest
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeType

/**
 * 集群节点服务接口
 */
interface ClusterNodeService {

    /**
     * 查询id为[id]的节点信息
     */
    fun getByClusterId(id: String): ClusterNodeInfo?

    /**
     * 查询id为[id]的节点名称
     */
    fun getClusterNameById(id: String): ClusterNodeName

    /**
     * 查询名称为[name]的节点信息
     */
    fun getByClusterName(name: String): ClusterNodeInfo?

    /**
     * 查询中心节点
     */
    fun getCenterNode(): ClusterNodeInfo

    /**
     * 查询边缘节点列表
     */
    fun listEdgeNodes(): List<ClusterNodeInfo>

    /**
     * 查询所有的集群节点
     * @param name 集群名称过滤，前缀匹配
     * @param type 集群类型过滤
     */
    fun listClusterNodes(name: String?, type: ClusterNodeType?): List<ClusterNodeInfo>

    /**
     * 分页查询节点列表
     *
     * @param option 列表选项
     */
    fun listClusterNodesPage(option: ClusterListOption): Page<ClusterNodeInfo>

    /**
     * 判断名称为[name]的集群节点是否存在
     */
    fun existClusterName(name: String): Boolean

    /**
     * 根据[request]创建集群节点，创建成功后返回集群节点信息
     */
    fun create(userId: String, request: ClusterNodeCreateRequest): ClusterNodeInfo

    /**
     * 根据集群[id]删除集群节点
     */
    fun deleteById(id: String)

    /**
     * 尝试连接远程集群，连接失败抛[ErrorCodeException]异常
     */
    @Throws(ErrorCodeException::class)
    fun tryConnect(name: String)

    /**
     * 根据[request]创建集群节点，创建成功后返回集群节点信息
     */
    fun updateClusterNodeStatus(request: ClusterNodeStatusUpdateRequest)
}

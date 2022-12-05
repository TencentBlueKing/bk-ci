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

package com.tencent.bkrepo.replication.replica.base

import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion

/**
 * 同步器
 */
interface Replicator {

    /**
     * 检查版本
     */
    fun checkVersion(context: ReplicaContext)

    /**
     * 同步项目
     */
    fun replicaProject(context: ReplicaContext)

    /**
     * 同步仓库
     */
    fun replicaRepo(context: ReplicaContext)

    /**
     * 同步包
     */
    fun replicaPackage(context: ReplicaContext, packageSummary: PackageSummary)

    /**
     * 同步包版本具体逻辑
     * @return 是否执行了同步，如果远程存在相同版本，则返回false
     */
    fun replicaPackageVersion(
        context: ReplicaContext,
        packageSummary: PackageSummary,
        packageVersion: PackageVersion
    ): Boolean

    /**
     * 同步文件
     * @return 是否执行了同步，如果远程存在相同文件，则返回false
     */
    fun replicaFile(context: ReplicaContext, node: NodeInfo): Boolean

    /**
     * 同步目录节点
     * @return 是否执行了同步，如果远程存在相同目录，则返回false
     */
    fun replicaDir(context: ReplicaContext, node: NodeInfo)
}

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

import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.replication.manager.LocalDataManager
import com.tencent.bkrepo.replication.pojo.record.ExecutionResult
import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus
import com.tencent.bkrepo.replication.pojo.record.request.RecordDetailInitialRequest
import com.tencent.bkrepo.replication.pojo.task.objects.PackageConstraint
import com.tencent.bkrepo.replication.pojo.task.objects.PathConstraint
import com.tencent.bkrepo.replication.pojo.task.setting.ErrorStrategy
import com.tencent.bkrepo.replication.service.ReplicaRecordService
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption

/**
 * 同步服务抽象类
 * 一次replica执行负责一个任务下的一个集群，在子线程中执行
 */
@Suppress("TooGenericExceptionCaught")
abstract class AbstractReplicaService(
    private val replicaRecordService: ReplicaRecordService,
    private val localDataManager: LocalDataManager
) : ReplicaService {

    /**
     * 同步整个仓库数据
     */
    protected fun replicaByRepo(replicaContext: ReplicaContext) {
        val context = initialExecutionContext(replicaContext)
        try {
            if (replicaContext.taskObject.repoType == RepositoryType.GENERIC) {
                // 同步generic节点
                val root = localDataManager.findNodeDetail(
                    projectId = replicaContext.localProjectId,
                    repoName = replicaContext.localRepoName,
                    fullPath = PathUtils.ROOT
                ).nodeInfo
                replicaByPath(context, root)
                return
            }
            // 同步包
            val option = PackageListOption(pageNumber = 1, pageSize = PAGE_SIZE)
            var packages = localDataManager.listPackagePage(
                projectId = replicaContext.localProjectId,
                repoName = replicaContext.localRepoName,
                option = option
            )
            while (packages.isNotEmpty()) {
                packages.forEach {
                    replicaByPackage(context, it)
                }
                option.pageNumber += 1
                packages = localDataManager.listPackagePage(
                    projectId = replicaContext.localProjectId,
                    repoName = replicaContext.localRepoName,
                    option = option
                )
            }
        } catch (throwable: Throwable) {
            setErrorStatus(context, throwable)
        } finally {
            completeRecordDetail(context)
        }
    }

    /**
     * 同步指定包的数据
     */
    protected fun replicaByPackageConstraint(replicaContext: ReplicaContext, constraint: PackageConstraint) {
        val context = initialExecutionContext(replicaContext, packageConstraint = constraint)
        try {
            // 查询本地包信息
            val packageSummary = localDataManager.findPackageByKey(
                projectId = replicaContext.localProjectId,
                repoName = replicaContext.taskObject.localRepoName,
                packageKey = constraint.packageKey
            )
            replicaByPackage(context, packageSummary, constraint.versions)
        } catch (throwable: Throwable) {
            setErrorStatus(context, throwable)
        } finally {
            completeRecordDetail(context)
        }
    }

    /**
     * 同步指定路径的数据
     */
    protected fun replicaByPathConstraint(replicaContext: ReplicaContext, constraint: PathConstraint) {
        val context = initialExecutionContext(replicaContext, pathConstraint = constraint)
        try {
            val nodeInfo = localDataManager.findNodeDetail(
                projectId = replicaContext.localProjectId,
                repoName = replicaContext.localRepoName,
                fullPath = constraint.path
            ).nodeInfo
            replicaByPath(context, nodeInfo)
        } catch (throwable: Throwable) {
            setErrorStatus(context, throwable)
        } finally {
            completeRecordDetail(context)
        }
    }

    /**
     * 同步路径
     * 采用广度优先遍历
     */
    private fun replicaByPath(context: ReplicaExecutionContext, node: NodeInfo) {
        with(context) {
            if (!node.folder) {
                replicaFile(context, node)
                return
            }
            // 查询子节点
            localDataManager.listNode(
                projectId = replicaContext.localProjectId,
                repoName = replicaContext.localRepoName,
                fullPath = node.fullPath
            ).forEach {
                replicaByPath(this, it)
            }
        }
    }

    /**
     * 同步节点
     */
    private fun replicaFile(context: ReplicaExecutionContext, node: NodeInfo) {
        with(context) {
            try {
                val executed = replicaContext.replicator.replicaFile(replicaContext, node)
                updateProgress(executed)
                return
            } catch (throwable: Throwable) {
                progress.failed += 1
                setErrorStatus(this, throwable)
                if (replicaContext.task.setting.errorStrategy == ErrorStrategy.FAST_FAIL) {
                    throw throwable
                }
            }
        }
    }

    /**
     * 根据[packageSummary]和版本列表[versionNames]执行同步
     */
    private fun replicaByPackage(
        context: ReplicaExecutionContext,
        packageSummary: PackageSummary,
        versionNames: List<String>? = null
    ) {
        with(context) {
            replicator.replicaPackage(replicaContext, packageSummary)
            val versions = versionNames?.map {
                localDataManager.findPackageVersion(
                    projectId = replicaContext.localProjectId,
                    repoName = replicaContext.localRepoName,
                    packageKey = packageSummary.key,
                    version = it
                )
            } ?: localDataManager.listAllVersion(
                projectId = replicaContext.localProjectId,
                repoName = replicaContext.localRepoName,
                packageKey = packageSummary.key,
                option = VersionListOption()
            )
            versions.forEach {
                replicaPackageVersion(this, packageSummary, it)
            }
        }
    }

    /**
     * 同步版本
     */
    private fun replicaPackageVersion(
        context: ReplicaExecutionContext,
        packageSummary: PackageSummary,
        version: PackageVersion
    ) {
        with(context) {
            try {
                val executed = replicator.replicaPackageVersion(replicaContext, packageSummary, version)
                updateProgress(executed)
            } catch (throwable: Throwable) {
                progress.failed += 1
                setErrorStatus(this, throwable)
                if (replicaContext.task.setting.errorStrategy == ErrorStrategy.FAST_FAIL) {
                    throw throwable
                }
            }
        }
    }

    /**
     * 初始化执行过程context
     */
    private fun initialExecutionContext(
        context: ReplicaContext,
        packageConstraint: PackageConstraint? = null,
        pathConstraint: PathConstraint? = null
    ): ReplicaExecutionContext {
        // 创建详情
        val request = RecordDetailInitialRequest(
            recordId = context.taskRecord.id,
            remoteCluster = context.remoteCluster.name,
            localRepoName = context.localRepoName,
            repoType = context.localRepoType,
            packageConstraint = packageConstraint,
            pathConstraint = pathConstraint
        )
        val recordDetail = replicaRecordService.initialRecordDetail(request)
        return ReplicaExecutionContext(context, recordDetail)
    }

    /**
     * 设置状态为失败状态
     */
    private fun setErrorStatus(context: ReplicaExecutionContext, throwable: Throwable) {
        context.status = ExecutionStatus.FAILED
        context.appendErrorReason(throwable.message.orEmpty())
        context.replicaContext.status = ExecutionStatus.FAILED
    }

    /**
     * 持久化同步进度
     */
    private fun completeRecordDetail(context: ReplicaExecutionContext) {
        with(context) {
            val result = ExecutionResult(
                status = status,
                progress = progress,
                errorReason = buildErrorReason()
            )
            replicaRecordService.completeRecordDetail(detail.id, result)
        }
    }

    companion object {
        private const val PAGE_SIZE = 1000
    }
}

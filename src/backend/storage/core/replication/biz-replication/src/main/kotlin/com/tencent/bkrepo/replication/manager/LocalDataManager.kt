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

package com.tencent.bkrepo.replication.manager

import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import com.tencent.bkrepo.repository.pojo.project.ProjectInfo
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.springframework.stereotype.Component
import java.io.InputStream

/**
 * 本地数据管理类
 * 用于访问本地集群数据
 */
@Component
class LocalDataManager(
    private val projectClient: ProjectClient,
    private val repositoryClient: RepositoryClient,
    private val nodeClient: NodeClient,
    private val packageClient: PackageClient,
    private val metadataClient: MetadataClient,
    private val storageService: StorageService
) {

    /**
     * 获取blob文件数据
     */
    @Throws(IllegalStateException::class)
    fun getBlobData(sha256: String, length: Long, repoInfo: RepositoryDetail): InputStream {
        val blob = storageService.load(sha256, Range.full(length), repoInfo.storageCredentials)
        check(blob != null) { "File data[sha256] does not exist" }
        return blob
    }

    /**
     * 查找项目
     * 项目不存在抛异常
     */
    @Throws(IllegalStateException::class)
    fun findProjectById(projectId: String): ProjectInfo {
        val project = projectClient.getProjectInfo(projectId).data
        check(project != null) { "Local project[$projectId] does not exist" }
        return project
    }

    /**
     * 判断项目是否存在
     */
    fun existProject(projectId: String): Boolean {
        return projectClient.getProjectInfo(projectId).data != null
    }

    /**
     * 查找仓库
     * 仓库不存在抛异常
     */
    @Throws(IllegalStateException::class)
    fun findRepoByName(projectId: String, repoName: String, type: String? = null): RepositoryDetail {
        val repo = repositoryClient.getRepoDetail(projectId, repoName, type).data
        check(repo != null) { "Local repository[$repoName] does not exist" }
        return repo
    }

    /**
     * 判断仓库是否存在
     */
    fun existRepo(projectId: String, repoName: String, type: String? = null): Boolean {
        return repositoryClient.getRepoDetail(projectId, repoName, type).data != null
    }

    /**
     * 根据packageKey查找包信息
     */
    @Throws(IllegalStateException::class)
    fun findPackageByKey(projectId: String, repoName: String, packageKey: String): PackageSummary {
        val packageSummary = packageClient.findPackageByKey(projectId, repoName, packageKey).data
        check(packageSummary != null) { "Local package[$packageKey] does not exist" }
        return packageSummary
    }

    /**
     * 查询所有版本
     */
    @Throws(IllegalStateException::class)
    fun listAllVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        option: VersionListOption
    ): List<PackageVersion> {
        val versions = packageClient.listAllVersion(projectId, repoName, packageKey, option).data
        check(versions != null) { "Local package [$packageKey] does not exist" }
        return versions
    }

    /**
     * 查询指定版本
     */
    @Throws(IllegalStateException::class)
    fun findPackageVersion(projectId: String, repoName: String, packageKey: String, version: String): PackageVersion {
        val packageVersion = packageClient.findVersionByName(projectId, repoName, packageKey, version).data
        check(packageVersion != null) { "Local package version [$version] does not exist" }
        return packageVersion
    }

    /**
     * 查找节点
     */
    @Throws(IllegalStateException::class)
    fun findNodeDetail(projectId: String, repoName: String, fullPath: String): NodeDetail {
        val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, fullPath).data
        check(nodeDetail != null) { "Local node path [$fullPath] does not exist" }
        return nodeDetail
    }

    /**
     * 分页查询包
     */
    @Throws(IllegalStateException::class)
    fun listPackagePage(projectId: String, repoName: String, option: PackageListOption): List<PackageSummary> {
        val packages = packageClient.listPackagePage(
            projectId = projectId,
            repoName = repoName,
            option = option
        ).data?.records
        check(packages != null) { "Local packages not found" }
        return packages
    }

    /**
     * 查询目录下的文件列表
     */
    fun listNode(projectId: String, repoName: String, fullPath: String): List<NodeInfo> {
        val nodes = nodeClient.listNode(
            projectId = projectId,
            repoName = repoName,
            path = fullPath,
            includeFolder = true,
            deep = false
        ).data
        check(nodes != null) { "Local packages not found" }
        return nodes
    }
}

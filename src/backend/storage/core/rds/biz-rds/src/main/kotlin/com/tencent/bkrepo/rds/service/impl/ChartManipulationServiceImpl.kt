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

package com.tencent.bkrepo.rds.service.impl

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.rds.constants.CHART
import com.tencent.bkrepo.rds.constants.FILE_TYPE
import com.tencent.bkrepo.rds.exception.RdsFileNotFoundException
import com.tencent.bkrepo.rds.listener.event.ChartDeleteEvent
import com.tencent.bkrepo.rds.listener.event.ChartVersionDeleteEvent
import com.tencent.bkrepo.rds.pojo.artifact.RdsArtifactInfo
import com.tencent.bkrepo.rds.pojo.artifact.RdsDeleteArtifactInfo
import com.tencent.bkrepo.rds.pojo.chart.ChartPackageDeleteRequest
import com.tencent.bkrepo.rds.pojo.chart.ChartVersionDeleteRequest
import com.tencent.bkrepo.rds.service.ChartManipulationService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChartManipulationServiceImpl : AbstractChartService(), ChartManipulationService {

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    override fun upload(artifactInfo: RdsArtifactInfo, artifactFileMap: ArtifactFileMap) {
        val keys = artifactFileMap.keys
        checkRepositoryExistAndCategory(artifactInfo)
        check(keys.contains(CHART)) {
            throw RdsFileNotFoundException(
                "no package file found in form fields chart"
            )
        }
        val context = ArtifactUploadContext(artifactFileMap[CHART]!!)
        context.putAttribute(FILE_TYPE, CHART)
        ArtifactContextHolder.getRepository().upload(context)
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    override fun deleteVersion(userId: String, artifactInfo: RdsDeleteArtifactInfo) {
        logger.info("handling delete chart version request: [$artifactInfo]")
        with(artifactInfo) {
            if (!packageVersionExist(projectId, repoName, packageName, version)) {
                throw RdsFileNotFoundException(
                    "remove package $packageName for version [$version] failed: no such file or directory"
                )
            }
            val context = ArtifactRemoveContext()
            repository.remove(context)
            when (context.repositoryDetail.category) {
                RepositoryCategory.REMOTE -> return
                else -> {
                    publishEvent(
                        ChartVersionDeleteEvent(
                            ChartVersionDeleteRequest(
                                projectId, repoName, PackageKeys.resolveRds(packageName), version, userId
                            )
                        )
                    )
                }
            }
        }
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    override fun deletePackage(userId: String, artifactInfo: RdsDeleteArtifactInfo) {
        logger.info("handling delete chart request: [$artifactInfo]")
        with(artifactInfo) {
            if (!packageExist(projectId, repoName, packageName)) {
                throw RdsFileNotFoundException("remove package $packageName failed: no such file or directory")
            }
            val context = ArtifactRemoveContext()
            repository.remove(context)
            when (context.repositoryDetail.category) {
                RepositoryCategory.REMOTE -> return
                else -> {
                    publishEvent(
                        ChartDeleteEvent(
                            ChartPackageDeleteRequest(projectId, repoName, PackageKeys.resolveRds(packageName), userId)
                        )
                    )
                }
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ChartManipulationServiceImpl::class.java)
    }
}

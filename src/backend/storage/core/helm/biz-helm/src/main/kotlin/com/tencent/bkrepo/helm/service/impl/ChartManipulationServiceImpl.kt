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

package com.tencent.bkrepo.helm.service.impl

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.helm.constants.CHART
import com.tencent.bkrepo.helm.constants.FILE_TYPE
import com.tencent.bkrepo.helm.constants.PROV
import com.tencent.bkrepo.helm.exception.HelmFileNotFoundException
import com.tencent.bkrepo.helm.pojo.artifact.HelmArtifactInfo
import com.tencent.bkrepo.helm.pojo.artifact.HelmDeleteArtifactInfo
import com.tencent.bkrepo.helm.service.ChartManipulationService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChartManipulationServiceImpl : AbstractChartService(), ChartManipulationService {

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    override fun upload(artifactInfo: HelmArtifactInfo, artifactFileMap: ArtifactFileMap) {
        val keys = artifactFileMap.keys
        checkRepositoryExistAndCategory(artifactInfo)
        check(keys.contains(CHART) || keys.contains(PROV)) {
            throw HelmFileNotFoundException(
                "no package or provenance file found in form fields chart and prov"
            )
        }
        if (keys.contains(CHART)) {
            val context = ArtifactUploadContext(artifactFileMap[CHART]!!)
            context.putAttribute(FILE_TYPE, CHART)
            ArtifactContextHolder.getRepository().upload(context)
        }
        if (keys.contains(PROV)) {
            val context = ArtifactUploadContext(artifactFileMap[PROV]!!)
            context.putAttribute(FILE_TYPE, PROV)
            ArtifactContextHolder.getRepository().upload(context)
        }
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    override fun uploadProv(artifactInfo: HelmArtifactInfo, artifactFileMap: ArtifactFileMap) {
        checkRepositoryExistAndCategory(artifactInfo)
        check(artifactFileMap.keys.contains(PROV)) {
            throw HelmFileNotFoundException("no provenance file found in form fields prov")
        }
        val context = ArtifactUploadContext(artifactFileMap[PROV]!!)
        context.putAttribute(FILE_TYPE, PROV)
        ArtifactContextHolder.getRepository().upload(context)
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    override fun deleteVersion(userId: String, artifactInfo: HelmDeleteArtifactInfo) {
        logger.info("handling delete chart version request: [$artifactInfo]")
        with(artifactInfo) {
            if (!packageVersionExist(projectId, repoName, packageName, version)) {
                throw HelmFileNotFoundException(
                    "remove package $packageName for version [$version] failed: no such file or directory"
                )
            }
            val context = ArtifactRemoveContext()
            ArtifactContextHolder.getRepository().remove(context)
        }
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    override fun deletePackage(userId: String, artifactInfo: HelmDeleteArtifactInfo) {
        logger.info("handling delete chart request: [$artifactInfo]")
        with(artifactInfo) {
            if (!packageExist(projectId, repoName, packageName)) {
                throw HelmFileNotFoundException("remove package $packageName failed: no such file or directory")
            }
            val context = ArtifactRemoveContext()
            ArtifactContextHolder.getRepository().remove(context)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ChartManipulationServiceImpl::class.java)
    }
}

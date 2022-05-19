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

package com.tencent.bkrepo.npm.handler

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.model.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.pojo.enums.NpmOperationAction
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.repository.api.PackageDependentsClient
import com.tencent.bkrepo.repository.pojo.dependent.PackageDependentsRelation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class NpmDependentHandler {

    @Autowired
    private lateinit var packageDependentsClient: PackageDependentsClient

    @Async
    fun updatePackageDependents(
        userId: String,
        artifactInfo: ArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData,
        action: NpmOperationAction
    ) {
        val latestVersion = NpmUtils.getLatestVersionFormDistTags(npmPackageMetaData.distTags)
        val versionMetaData = npmPackageMetaData.versions.map[latestVersion]!!

        when (action) {
            NpmOperationAction.PUBLISH -> {
                doDependentWithPublish(userId, artifactInfo, versionMetaData)
            }
            NpmOperationAction.UNPUBLISH -> {
                doDependentWithUnPublish(userId, artifactInfo, versionMetaData)
            }
            NpmOperationAction.MIGRATION -> {
                doDependentWithPublish(userId, artifactInfo, versionMetaData)
            }
            else -> {
                logger.warn("don't find operation action [${action.name}].")
            }
        }
    }

    private fun doDependentWithPublish(
        userId: String,
        artifactInfo: ArtifactInfo,
        versionMetaData: NpmVersionMetadata
    ) {
        val name = versionMetaData.name.orEmpty()
        with(artifactInfo){
            val relation = PackageDependentsRelation(
                projectId = projectId,
                repoName = repoName,
                packageKey = name,
                dependencies = versionMetaData.dependencies?.keys.orEmpty().map { PackageKeys.ofNpm(it) }.toSet()
            )
            packageDependentsClient.addDependents(relation)
            logger.info("user [$userId] publish dependent for package: [$name] success.")
        }
    }

    private fun doDependentWithUnPublish(
        userId: String,
        artifactInfo: ArtifactInfo,
        versionMetaData: NpmVersionMetadata
    ) {
        val name = versionMetaData.name.orEmpty()

        with(artifactInfo){
            val relation = PackageDependentsRelation(
                projectId = projectId,
                repoName = repoName,
                packageKey = name,
                dependencies = versionMetaData.dependencies?.keys.orEmpty().map { PackageKeys.ofNpm(it) }.toSet()
            )
            packageDependentsClient.reduceDependents(relation)
            logger.info("user [$userId] delete dependent for package: [$name] success.")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NpmDependentHandler::class.java)
    }
}

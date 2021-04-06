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

package com.tencent.bkrepo.repository.service.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.repository.dao.PackageDao
import com.tencent.bkrepo.repository.dao.PackageVersionDao
import com.tencent.bkrepo.repository.model.TPackageVersion
import com.tencent.bkrepo.repository.pojo.stage.ArtifactStageEnum
import com.tencent.bkrepo.repository.pojo.stage.StageUpgradeRequest
import com.tencent.bkrepo.repository.service.StageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 制品晋级服务接口实现类
 */
@Service
class StageServiceImpl(
    private val packageDao: PackageDao,
    private val packageVersionDao: PackageVersionDao
) : StageService {

    override fun query(projectId: String, repoName: String, packageKey: String, version: String): List<String> {
        return findPackageVersion(projectId, repoName, packageKey, version).stageTag
    }

    override fun upgrade(request: StageUpgradeRequest) {
        with(request) {
            val packageVersion = findPackageVersion(projectId, repoName, packageKey, version)
            try {
                val oldStage = ArtifactStageEnum.ofTagOrDefault(packageVersion.stageTag.lastOrNull())
                val newStage = ArtifactStageEnum.ofTagOrNull(newTag).let { oldStage.upgrade(it) }
                val newStageTag = packageVersion.stageTag.toMutableList().apply { add(newStage.tag) }
                packageVersion.stageTag = newStageTag
                packageVersion.lastModifiedDate = LocalDateTime.now()
                packageVersion.lastModifiedBy = operator
                packageVersionDao.save(packageVersion)
                logger.info("Upgrade stage[$packageKey] from $oldStage to $newStage success")
            } catch (exception: IllegalArgumentException) {
                throw ErrorCodeException(ArtifactMessageCode.STAGE_UPGRADE_ERROR, exception.message.orEmpty())
            }
        }
    }

    private fun findPackageVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ): TPackageVersion {
        val tPackage = packageDao.findByKey(projectId, repoName, packageKey)
            ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, packageKey)
        return packageVersionDao.findByName(tPackage.id!!, version)
            ?: throw ErrorCodeException(CommonMessageCode.RESOURCE_NOT_FOUND, version)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StageServiceImpl::class.java)
    }
}

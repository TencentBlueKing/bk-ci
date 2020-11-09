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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.service

import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.service.s3.S3Service
import com.tencent.devops.project.service.tof.TOFService
import com.tencent.devops.project.util.ProjectUtils.packagingBean
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectS3Service @Autowired constructor(
    private val s3Service: S3Service,
    private val projectDao: ProjectDao,
    private val tofService: TOFService,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val repoGray: RepoGray,
    private val bkRepoClient: BkRepoClient,
    private val projectService: ProjectService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectS3Service::class.java)
    }

    fun createCodeCCScanProject(userId: String, projectCreateInfo: ProjectCreateInfo): ProjectVO {
        logger.info("start to create public scan project!")
        var publicScanProject = projectDao.getByEnglishName(dslContext, projectCreateInfo.englishName)
        if (null != publicScanProject) {
            return packagingBean(publicScanProject, setOf())
        }

        try {
            projectService.create(
                    userId = userId,
                    projectCreateInfo = projectCreateInfo,
                    accessToken = null,
                    isUserProject = false
            )
//            val logoFile = drawImage(projectCreateInfo.englishName.substring(0, 1).toUpperCase())
//            try {
//                // 发送服务器
//                val logoAddress = s3Service.saveLogo(logoFile, projectCreateInfo.englishName)
//                val userDeptDetail = tofService.getUserDeptDetail(userId, "")
//                logger.info("get user dept info successfully!")
//
//                val createSuccess = bkRepoClient.createBkRepoResource(userId, projectCreateInfo.englishName)
//                logger.info("create bkrepo project ${projectCreateInfo.englishName} success: $createSuccess")
//                if (createSuccess) {
//                    repoGray.addGrayProject(projectCreateInfo.englishName, redisOperation)
//                    logger.info("add project ${projectCreateInfo.englishName} to repoGrey")
//                }
//
//                projectDao.create(
//                    dslContext, userId, logoAddress, projectCreateInfo, userDeptDetail,
//                    projectCreateInfo.englishName, ProjectChannelCode.BS
//                )
//            } finally {
//                if (logoFile.exists()) {
//                    logoFile.delete()
//                }
//            }
        } catch (e: Throwable) {
            logger.error("Create project failed,", e)
            throw e
        }

        publicScanProject = projectDao.getByEnglishName(dslContext, projectCreateInfo.englishName)
        logger.info("create public scan project successfully!")
        return packagingBean(publicScanProject!!, setOf())
    }
}
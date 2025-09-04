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

package com.tencent.devops.project.service

import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectExtSystemTagDTO
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectCodeCCScanFacadeService @Autowired constructor(
    private val projectService: ProjectService,
    val projectTagService: ProjectTagService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectCodeCCScanFacadeService::class.java)
        private const val CODECC_SYSTEM_CODE = "CODECC"
    }

    fun createCodeCCScanProject(
        userId: String,
        tag: String? = null,
        codeccTag: String? = null,
        projectCreateInfo: ProjectCreateInfo
    ): ProjectVO {
        logger.info("createCodeCCScanProject|${projectCreateInfo.englishName}|${projectCreateInfo.projectName}|$userId")
        var publicScanProject = projectService.getByEnglishName(projectCreateInfo.englishName)
        if (null != publicScanProject) {
            return publicScanProject
        }

        try {
            projectService.create(
                userId = userId,
                projectCreateInfo = projectCreateInfo,
                accessToken = null,
                createExtInfo = ProjectCreateExtInfo(needValidate = false, needAuth = false),
                defaultProjectId = projectCreateInfo.englishName,
                projectChannel = ProjectChannelCode.CODECC
            )

            // codecc任务自动将流量指向auto集群
            projectTagService.updateTagByProject(projectCreateInfo.englishName, tag)

            // codecc任务自己系统的路由Tag
            if (!codeccTag.isNullOrEmpty()) {
                projectTagService.updateExtSystemRouterTag(
                    ProjectExtSystemTagDTO(
                        routerTag = codeccTag,
                        projectCodeList = listOf(projectCreateInfo.englishName),
                        system = CODECC_SYSTEM_CODE
                    )
                )
            }
        } catch (e: Throwable) {
            logger.error("CodeCC_Scan_Project_Create_Failed| error=${e.message}", e)
            throw e
        }

        publicScanProject = projectService.getByEnglishName(projectCreateInfo.englishName)
        logger.info("create public scan project successfully!")
        return publicScanProject!!
    }
}

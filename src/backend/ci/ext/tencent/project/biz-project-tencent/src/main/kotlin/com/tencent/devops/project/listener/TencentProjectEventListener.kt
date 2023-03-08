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

package com.tencent.devops.project.listener

import com.tencent.devops.common.auth.api.BSAuthTokenApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.project.pojo.ProjectUpdateLogoInfo
import com.tencent.devops.project.pojo.mq.ProjectBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateLogoBroadCastEvent
import com.tencent.devops.project.service.ProjectPaasCCService
import com.tencent.devops.project.service.iam.IamV3Service
import com.tencent.devops.project.service.impl.AbsOpProjectServiceImpl.Companion.logger
import org.springframework.beans.factory.annotation.Autowired

/**
 * deng
 * 2019-12-17
 */
@Suppress("UNUSED", "TooGenericExceptionCaught")
class TencentProjectEventListener @Autowired constructor(
    val projectPaasCCService: ProjectPaasCCService,
    val bsAuthTokenApi: BSAuthTokenApi,
    val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    @Autowired(required = false)
    val iamV3Service: IamV3Service?
) : ProjectEventListener {

    override fun execute(event: ProjectBroadCastEvent) {
        try {
            when (event) {
                is ProjectCreateBroadCastEvent -> {
                    onReceiveProjectCreate(event)
                }
                is ProjectUpdateBroadCastEvent -> {
                    onReceiveProjectUpdate(event)
                }
                is ProjectUpdateLogoBroadCastEvent -> {
                    onReceiveProjectUpdateLogo(event)
                }
                is TxIamV3CreateEvent -> {
                    iamV3Service?.createIamV3Project(event)
                }
            }
        } catch (error: Exception) {
            logger.error("BKSystemMonitor| project listener execute error", error)
        }
    }

    // 已改成同步，无需重复添加
    override fun onReceiveProjectCreate(event: ProjectCreateBroadCastEvent) {
//        val accessToken = bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
        // 过渡期间让新建项目直接设置为灰度v2
//        opProjectService.setGrayProject(projectCodeList = listOf(event.projectInfo.englishName), operateFlag = 1)
//        projectPaasCCService.createPaasCCProject(
//            userId = event.userId,
//            projectId = event.projectId,
//            accessToken = accessToken,
//            projectCreateInfo = event.projectInfo
//        )
    }

    override fun onReceiveProjectUpdate(event: ProjectUpdateBroadCastEvent) {
        val accessToken = bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
        projectPaasCCService.updatePaasCCProject(
            userId = event.userId,
            projectId = event.projectId,
            projectUpdateInfo = event.projectInfo,
            accessToken = accessToken
        )
    }

    override fun onReceiveProjectUpdateLogo(event: ProjectUpdateLogoBroadCastEvent) {
        val accessToken = bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)

        val projectUpdateLogoInfo = ProjectUpdateLogoInfo(
            logo_addr = event.logoAddr,
            updator = event.userId
        )
        projectPaasCCService.updatePaasCCProjectLogo(
            userId = event.userId,
            projectId = event.projectId,
            accessToken = accessToken,
            projectUpdateLogoInfo = projectUpdateLogoInfo
        )
    }
}

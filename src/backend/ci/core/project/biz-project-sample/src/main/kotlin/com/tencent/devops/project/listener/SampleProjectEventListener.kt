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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.enum.ProjectEventType
import com.tencent.devops.project.pojo.ProjectCallbackData
import com.tencent.devops.project.pojo.ProjectUpdateLogoInfo
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectEnableStatusBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateLogoBroadCastEvent
import com.tencent.devops.project.service.ProjectCallbackControl
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

class SampleProjectEventListener @Autowired constructor(
    val projectCallbackControl: ProjectCallbackControl,
    private val projectDao: ProjectDao,
    private val dslContext: DSLContext
) : ProjectEventListener {

    override fun onReceiveProjectCreate(event: ProjectCreateBroadCastEvent) {
        projectCallbackControl.callBackProjectEvent(
            projectEventType = ProjectEventType.CREATE,
            callbackData = ProjectCallbackData(
                event = ProjectEventType.CREATE,
                createInfo = event.projectInfo,
                userId = event.userId,
                projectId = event.projectId,
                projectEnglishName = event.projectInfo.englishName
            )
        )
    }

    override fun onReceiveProjectUpdate(event: ProjectUpdateBroadCastEvent) {
        projectCallbackControl.callBackProjectEvent(
            projectEventType = ProjectEventType.UPDATE,
            callbackData = ProjectCallbackData(
                event = ProjectEventType.UPDATE,
                updateInfo = event.projectInfo,
                userId = event.userId,
                projectId = event.projectId,
                projectEnglishName = event.projectInfo.englishName
            )
        )
    }

    override fun onReceiveProjectUpdateLogo(event: ProjectUpdateLogoBroadCastEvent) {
        val projectInfo = projectDao.get(dslContext, event.projectId) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
            params = arrayOf(event.projectId),
            defaultMessage = "project - ${event.projectId} is not exist!"
        )
        projectCallbackControl.callBackProjectEvent(
            projectEventType = ProjectEventType.UPDATE_LOGO,
            callbackData = ProjectCallbackData(
                event = ProjectEventType.UPDATE_LOGO,
                updateLogo = ProjectUpdateLogoInfo(
                    logo_addr = event.logoAddr,
                    updator = event.userId
                ),
                userId = event.userId,
                projectId = event.projectId,
                projectEnglishName = projectInfo.englishName
            )
        )
    }

    override fun onReceiveProjectEnable(event: ProjectEnableStatusBroadCastEvent) {
        // 此处的projectId为项目的英文名
        // 参考：com.tencent.devops.project.service.impl.AbsProjectServiceImpl.updateUsableStatus
        val projectInfo = projectDao.getByEnglishName(dslContext, event.projectId) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
            params = arrayOf(event.projectId),
            defaultMessage = "project ${event.projectId} is not exist"
        )
        projectCallbackControl.callBackProjectEvent(
            projectEventType = ProjectEventType.ENABLE,
            callbackData = ProjectCallbackData(
                event = ProjectEventType.ENABLE,
                enabled = event.enabled,
                userId = event.userId,
                projectId = projectInfo.englishName,
                projectEnglishName = event.projectId
            )
        )
    }
}

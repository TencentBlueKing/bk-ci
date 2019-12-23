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

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.web.mq.EXCHANGE_PAASCC_PROJECT_UPDATE
import com.tencent.devops.common.web.mq.ROUTE_PAASCC_PROJECT_UPDATE
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.ProjectLabelRelDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.pojo.OpGrayProject
import com.tencent.devops.project.pojo.OpProjectUpdateInfoRequest
import com.tencent.devops.project.pojo.PaasCCUpdateProject
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.Result
import org.jooq.DSLContext
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class OpProjectServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val projectLabelRelDao: ProjectLabelRelDao,
    private val rabbitTemplate: RabbitTemplate,
    private val redisOperation: RedisOperation,
    private val gray: Gray,
    private val projectDispatcher: ProjectDispatcher
) : AbsOpProjectServiceImpl(dslContext, projectDao, projectLabelRelDao, redisOperation, gray, projectDispatcher) {
    override fun listGrayProject(): Result<OpGrayProject> {
        return super.listGrayProject()
    }

    override fun setGrayProject(projectCodeList: List<String>, operateFlag: Int): Boolean {
        return super.setGrayProject(projectCodeList, operateFlag)
    }

    override fun updateProjectFromOp(userId: String, accessToken: String, projectInfoRequest: OpProjectUpdateInfoRequest): Int {
        val count = super.updateProjectFromOp(userId, accessToken, projectInfoRequest)
        val dbProjectRecord = projectDao.get(dslContext, projectInfoRequest.projectId)
        rabbitTemplate.convertAndSend(
            EXCHANGE_PAASCC_PROJECT_UPDATE,
            ROUTE_PAASCC_PROJECT_UPDATE, PaasCCUpdateProject(
            userId = userId,
            accessToken = accessToken,
            projectId = projectInfoRequest.projectId,
            retryCount = 0,
            projectUpdateInfo = ProjectUpdateInfo(
                projectName = projectInfoRequest.projectName,
                projectType = projectInfoRequest.projectType,
                bgId = projectInfoRequest.bgId,
                bgName = projectInfoRequest.bgName,
                centerId = projectInfoRequest.centerId,
                centerName = projectInfoRequest.centerName,
                deptId = projectInfoRequest.deptId,
                deptName = projectInfoRequest.deptName,
                description = dbProjectRecord!!.description ?: "",
                englishName = dbProjectRecord!!.englishName,
                ccAppId = projectInfoRequest.ccAppId,
                ccAppName = projectInfoRequest.cc_app_name,
                kind = projectInfoRequest.kind
//                        secrecy = projectInfoRequest.secrecyFlag
            )
        )
        )
        return count
    }

    override fun getProjectList(projectName: String?, englishName: String?, projectType: Int?, isSecrecy: Boolean?, creator: String?, approver: String?, approvalStatus: Int?, offset: Int, limit: Int, grayFlag: Boolean): Result<Map<String, Any?>?> {
        return super.getProjectList(projectName, englishName, projectType, isSecrecy, creator, approver, approvalStatus, offset, limit, grayFlag)
    }

    override fun getProjectCount(projectName: String?, englishName: String?, projectType: Int?, isSecrecy: Boolean?, creator: String?, approver: String?, approvalStatus: Int?, grayFlag: Boolean): Result<Int> {
        return super.getProjectCount(projectName, englishName, projectType, isSecrecy, creator, approver, approvalStatus, grayFlag)
    }
}

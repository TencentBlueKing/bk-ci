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
package com.tencent.devops.openapi.resources.apigw

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.ApigwProjectResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwProjectResourceImpl @Autowired constructor(private val client: Client) :
    ApigwProjectResource {

    override fun getProjectByGroup(
        appCode: String?,
        apigwType: String?,
        userId: String,
        bgName: String?,
        deptName: String?,
        centerName: String
    ): Result<List<ProjectVO>> {
        logger.info("Get  projects info by group ,userId:$userId,bgName:$bgName,deptName:$deptName,centerName:$centerName")
        return client.get(ServiceTxProjectResource::class).getProjectByGroup(
            userId = userId,
            bgName = bgName,
            deptName = deptName,
            centerName = centerName
        )
    }

    override fun getProject(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String
    ): Result<ProjectVO?> {
        logger.info("Get a project info ,projectId:$projectId")
        return client.get(ServiceProjectResource::class).get(projectId)
    }

    override fun getProjectByUser(
        appCode: String?,
        apigwType: String?,
        userId: String
    ): Result<List<ProjectVO>> {
        logger.info("Get user's project info ,userId:$userId")
        return client.get(ServiceProjectResource::class).list(userId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwProjectResourceImpl::class.java)
    }
}

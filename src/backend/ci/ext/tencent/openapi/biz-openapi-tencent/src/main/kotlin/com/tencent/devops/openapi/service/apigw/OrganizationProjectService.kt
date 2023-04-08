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
package com.tencent.devops.openapi.service.apigw

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.tx.util.OrganizationUtil
import com.tencent.devops.common.api.constant.OpenAPIMessageCode.ERROR_OPENAPI_INNER_SERVICE_FAIL
import com.tencent.devops.openapi.exception.MicroServiceInvokeFailure
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * @Description
 * @Date 2019/9/1
 * @Version 1.0
 */
@Service
class OrganizationProjectService(private val client: Client) {

    companion object {
        private val logger = LoggerFactory.getLogger(OrganizationProjectService::class.java)
    }

    fun getProjectIdsByOrganizationTypeAndId(
        userId: String,
        organizationType: String,
        organizationId: Long,
        deptName: String?,
        centerName: String?,
        interfaceName: String? = "OrganizationProjectService"
    ): Set<String> {
        logger.info("$interfaceName:getProjectIdsByOrganizationTypeAndId:Input($userId,$organizationType,$organizationId,$deptName,$centerName)")
        val projectIds = when (organizationType) {
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG -> {
                client.get(ServiceTxProjectResource::class).getProjectEnNamesByOrganization(
                    userId = userId,
                    bgId = organizationId,
                    deptName = deptName,
                    centerName = centerName
                )
            }
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT -> {
                client.get(ServiceTxProjectResource::class).getProjectEnNamesByDeptIdAndCenterName(
                    userId = userId,
                    deptId = organizationId,
                    centerName = centerName
                )
            }
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER -> {
                client.get(ServiceTxProjectResource::class).getProjectEnNamesByCenterId(
                    userId = userId,
                    centerId = organizationId
                )
            }
            else -> {
                throw InvalidParamException(
                    message = "organizationType not supported, only [$AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG,$AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT,$AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER] supported",
                    params = arrayOf(organizationType)
                )
            }
        }.data?.toSet() ?: emptySet()
        logger.info("$interfaceName:getProjectIdsByOrganizationTypeAndId:Output:$projectIds")
        return projectIds
    }

    fun getProjectIdsByOrganizationTypeAndName(
        userId: String,
        organizationType: String,
        organizationName: String,
        deptName: String?,
        centerName: String?,
        interfaceName: String? = "OrganizationProjectService"
    ): Set<String> {
        logger.info("$interfaceName:getProjectIdsByOrganizationTypeAndName:Input($userId,$organizationType,$organizationName,$deptName,$centerName)")
        // 1.根据组织信息获取所有项目
        val organization = OrganizationUtil.fillOrganization(
            organizationType = organizationType,
            organizationName = organizationName,
            deptName = deptName,
            centerName = centerName
        )
        val projectsResult = client.get(ServiceTxProjectResource::class).getProjectByGroup(
            userId = userId,
            bgName = organization.bgName,
            deptName = organization.deptName,
            centerName = organization.centerName
        )
        // 项目接口内容判空
        if (projectsResult.isNotOk()) {
            val resultStr = JsonUtil.toJson(projectsResult)
            val serviceInfo = "project:ServiceProjectResource:getProjectByGroup"
            throw MicroServiceInvokeFailure(
                serviceInterface = serviceInfo,
                message = "projectsResult=$resultStr",
                errorCode = ERROR_OPENAPI_INNER_SERVICE_FAIL,
                params = arrayOf(serviceInfo)
            )
        }
        // 2.根据所有项目Id获取对应流水线
        val projectIds = projectsResult.data!!.map { it.englishName }.toSet()
        logger.info("$interfaceName:getProjectIdsByOrganizationTypeAndName:Output:$projectIds")
        return projectIds
    }
}

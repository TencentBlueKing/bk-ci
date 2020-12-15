package com.tencent.devops.auth.service

import com.tencent.devops.auth.pojo.OrganizationEntity
import com.tencent.devops.auth.pojo.UpLevel
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectOrganizationResource
import com.tencent.devops.project.pojo.enums.OrganizationType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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

@Service
class OrganizationServiceImpl @Autowired constructor(
    val client: Client
) : OrganizationService {
    override fun getParentOrganizationInfo(organizationId: String, level: Int): List<OrganizationEntity>? {
        val upLevel = UpLevel.getUplevel(level)
        val parentDeptInfos = client.get(ServiceProjectOrganizationResource::class).getParentDeptInfos(organizationId, upLevel.ordinal).data
            ?: return null
        val result = mutableListOf<OrganizationEntity>()
        parentDeptInfos.forEach {
            result.add(
                OrganizationEntity(
                    organizationId = it.id.toInt(),
                    organizationName = it.name,
                    level = it.level.toInt()
                )
            )
        }
        return result
    }

    override fun getOrganizationInfo(organizationId: String, level: Int): OrganizationEntity? {
        logger.info("getOrganizationInfo $organizationId $level")
        val deptType =
            when (level) {
                1 -> OrganizationType.bg
                2 -> OrganizationType.dept
                3 -> OrganizationType.center
                else -> OrganizationType.bg
            }

        val deptInfos = client.get(ServiceProjectOrganizationResource::class).getOrganizations(
            userId = "",
            type = deptType,
            id = organizationId.toInt()
        ).data
        logger.info("getOrganizationInfo $organizationId $level| $deptInfos")
        if (deptInfos == null || deptInfos.isEmpty()) {
            return null
        }

        val deptInfo = deptInfos[0]
        return OrganizationEntity(
            organizationName = deptInfo.name!!,
            organizationId = deptInfo.id!!.toInt(),
            level = level
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}

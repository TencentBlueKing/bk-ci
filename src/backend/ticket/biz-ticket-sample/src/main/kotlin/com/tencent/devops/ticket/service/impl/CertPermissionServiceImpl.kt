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

package com.tencent.devops.ticket.service.impl

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.auth.api.BkAuthResourceType
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.ticket.dao.CertDao
import com.tencent.devops.ticket.service.CertPermissionService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class CertPermissionServiceImpl @Autowired constructor(
    private val certDao: CertDao,
    private val dslContext: DSLContext,
    private val authResourceApi: AuthResourceApi,
    private val authPermissionApi: AuthPermissionApi,
    private val ticketAuthServiceCode: TicketAuthServiceCode
) : CertPermissionService {

    private val resourceType = BkAuthResourceType.TICKET_CERT

    override fun validatePermission(
        userId: String,
        projectId: String,
        bkAuthPermission: BkAuthPermission,
        message: String
    ) {
        if (!validatePermission(userId, projectId, bkAuthPermission)) {
            throw CustomException(Response.Status.FORBIDDEN, message)
        }
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        resourceCode: String,
        bkAuthPermission: BkAuthPermission,
        message: String
    ) {
        if (!validatePermission(userId, projectId, resourceCode, bkAuthPermission)) {
            throw CustomException(Response.Status.FORBIDDEN, message)
        }
    }

    override fun validatePermission(userId: String, projectId: String, bkAuthPermission: BkAuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = ticketAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            permission = bkAuthPermission
        )
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        resourceCode: String,
        bkAuthPermission: BkAuthPermission
    ): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = ticketAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            resourceCode = resourceCode,
            permission = bkAuthPermission
        )
    }

    override fun filterCert(userId: String, projectId: String, bkAuthPermission: BkAuthPermission): List<String> {
        return authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = ticketAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            permission = bkAuthPermission,
            supplier = supplierForPermission(projectId)
        )
    }

    override fun filterCerts(
        userId: String,
        projectId: String,
        bkAuthPermissions: Set<BkAuthPermission>
    ): Map<BkAuthPermission, List<String>> {
        return authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = ticketAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            permissions = bkAuthPermissions,
            supplier = supplierForPermission(projectId)
        )
    }

    private fun supplierForPermission(projectId: String): () -> MutableList<String> {
        return {
            val fakeList = mutableListOf<String>()
            certDao.listIdByProject(
                dslContext = dslContext,
                projectId = projectId,
                offset = 0,
                limit = 500
            ).forEach {
                fakeList.add(it.toString())
            }
            fakeList
        }
    }

    override fun createResource(userId: String, projectId: String, certId: String) {
        authResourceApi.createResource(userId, ticketAuthServiceCode, resourceType, projectId, certId, certId)
    }

    override fun deleteResource(projectId: String, certId: String) {
        authResourceApi.deleteResource(ticketAuthServiceCode, resourceType, projectId, certId)
    }
}
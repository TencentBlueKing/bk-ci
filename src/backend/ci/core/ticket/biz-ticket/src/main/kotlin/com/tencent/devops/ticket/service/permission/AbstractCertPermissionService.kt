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

package com.tencent.devops.ticket.service.permission

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.ticket.service.CertPermissionService

abstract class AbstractCertPermissionService constructor(
    private val authResourceApi: AuthResourceApi,
    val authPermissionApi: AuthPermissionApi,
    val ticketAuthServiceCode: TicketAuthServiceCode
) : CertPermissionService {

    private val resourceType = AuthResourceType.TICKET_CERT

    override fun validatePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!validatePermission(userId, projectId, authPermission)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        resourceCode: String,
        authPermission: AuthPermission,
        message: String
    ) {
        if (authPermission == AuthPermission.VIEW)
            return
        if (!validatePermission(userId, projectId, resourceCode, authPermission)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validatePermission(userId: String, projectId: String, authPermission: AuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = ticketAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            permission = authPermission
        )
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        resourceCode: String,
        authPermission: AuthPermission
    ): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = ticketAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            resourceCode = resourceCode,
            permission = authPermission
        )
    }

    override fun filterCert(userId: String, projectId: String, authPermission: AuthPermission): List<String> {
        return authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = ticketAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            permission = authPermission,
            supplier = supplierForPermission(projectId)
        )
    }

    override fun filterCerts(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        return authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = ticketAuthServiceCode,
            resourceType = resourceType,
            projectCode = projectId,
            permissions = authPermissions,
            supplier = supplierForPermission(projectId)
        )
    }

    abstract fun supplierForPermission(projectId: String): () -> MutableList<String>

    override fun createResource(userId: String, projectId: String, certId: String) {
        authResourceApi.createResource(userId, ticketAuthServiceCode, resourceType, projectId, certId, certId)
    }

    override fun deleteResource(projectId: String, certId: String) {
        authResourceApi.deleteResource(ticketAuthServiceCode, resourceType, projectId, certId)
    }
}

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.ticket.service

import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialInfo
import com.tencent.devops.ticket.pojo.CredentialItemVo
import com.tencent.devops.ticket.pojo.CredentialSettingUpdate
import com.tencent.devops.ticket.pojo.CredentialUpdate
import com.tencent.devops.ticket.pojo.CredentialWithPermission
import com.tencent.devops.ticket.pojo.enums.CredentialType

@Suppress("ALL")
interface CredentialService {

    fun userCreate(userId: String, projectId: String, credential: CredentialCreate, authGroupList: List<BkAuthGroup>?)

    fun userEdit(userId: String, projectId: String, credentialId: String, credential: CredentialUpdate)

    fun userSettingEdit(
        userId: String,
        projectId: String,
        credentialId: String,
        credentialSetting: CredentialSettingUpdate
    ): Boolean

    fun userDelete(userId: String, projectId: String, credentialId: String)

    fun userList(
        userId: String,
        projectId: String,
        credentialTypes: List<CredentialType>?,
        offset: Int,
        limit: Int,
        keyword: String?
    ): SQLPage<CredentialWithPermission>

    fun hasPermissionList(
        userId: String,
        projectId: String,
        credentialTypes: List<CredentialType>?,
        authPermission: AuthPermission,
        offset: Int?,
        limit: Int?,
        keyword: String?
    ): SQLPage<Credential>

    fun serviceList(projectId: String, offset: Int, limit: Int): SQLPage<Credential>

    fun serviceCheck(projectId: String, credentialId: String)

    fun userShow(userId: String, projectId: String, credentialId: String): CredentialWithPermission

    fun userGet(userId: String, projectId: String, credentialId: String): CredentialWithPermission

    fun buildGet(
        projectId: String,
        buildId: String,
        credentialId: String,
        publicKey: String,
        taskId: String? = null,
        padding: Boolean
    ): CredentialInfo?

    fun buildGetAcrossProject(
        projectId: String,
        targetProjectId: String,
        buildId: String,
        credentialId: String,
        publicKey: String,
        padding: Boolean
    ): CredentialInfo?

    fun buildGetDetail(projectId: String, buildId: String, taskId: String?, credentialId: String): Map<String, String>

    fun serviceGet(
        projectId: String,
        credentialId: String,
        publicKey: String,
        padding: Boolean
    ): CredentialInfo?

    fun serviceGetAcrossProject(
        targetProjectId: String,
        credentialId: String,
        publicKey: String,
        padding: Boolean
    ): CredentialInfo?

    fun serviceGet(projectId: String, credentialId: String): Credential

    /**
     * 修改凭证的服务接口
     */
    fun serviceEdit(userId: String? = null, projectId: String, credentialId: String, credential: CredentialUpdate)

    fun getCredentialByIds(projectId: String?, credentialIds: Set<String>): List<Credential>?

    fun searchByCredentialId(projectId: String, offset: Int, limit: Int, credentialId: String): SQLPage<Credential>

    fun getCredentialItem(
        projectId: String,
        credentialId: String,
        publicKey: String,
        padding: Boolean
    ): CredentialItemVo?
}

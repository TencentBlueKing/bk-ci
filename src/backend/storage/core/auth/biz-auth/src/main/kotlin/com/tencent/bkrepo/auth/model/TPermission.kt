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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.auth.model

import com.tencent.bkrepo.auth.pojo.PermissionSet
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import java.time.LocalDateTime
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document

/**
 * 角色
 */
@Document("permission")
@CompoundIndexes(
    CompoundIndex(name = "repos_idx", def = "{'repos': 1}", background = true),
    CompoundIndex(name = "resourceType_idx", def = "{'resourceType': 1}", background = true),
    CompoundIndex(name = "projectId_idx", def = "{'projectId': 1}", background = true),
    CompoundIndex(name = "includePattern_idx", def = "{'includePattern': 1}", background = true),
    CompoundIndex(name = "excludePattern_idx", def = "{'excludePattern': 1}", background = true),
    CompoundIndex(name = "users_id_idx", def = "{'users.id': 1}", background = true),
    CompoundIndex(name = "users_action_idx", def = "{'users.action': 1}", background = true),
    CompoundIndex(name = "roles_id_idx", def = "{'roles.id': 1}", background = true),
    CompoundIndex(name = "roles_action_idx", def = "{'roles.action': 1}", background = true)
)

data class TPermission(
    val id: String? = null,
    var resourceType: ResourceType,
    var projectId: String? = null,
    var permName: String,
    var repos: List<String> = emptyList(),
    var includePattern: List<String> = emptyList(),
    var excludePattern: List<String> = emptyList(),
    var createBy: String,
    val createAt: LocalDateTime,
    var updatedBy: String,
    val updateAt: LocalDateTime,
    var users: List<PermissionSet> = emptyList(),
    var roles: List<PermissionSet> = emptyList()
)

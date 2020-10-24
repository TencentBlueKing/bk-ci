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

package com.tencent.bkrepo.auth.service.local

import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TRole
import com.tencent.bkrepo.auth.pojo.CreateRoleRequest
import com.tencent.bkrepo.auth.pojo.Role
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "local")
class RoleServiceImpl @Autowired constructor(
    private val roleRepository: RoleRepository
) : RoleService {

    override fun createRole(request: CreateRoleRequest): String? {
        logger.info("create  role  request : [$request] ")
        var role: TRole? = if (request.type == RoleType.REPO) {
            roleRepository.findFirstByRoleIdAndProjectIdAndRepoName(
                request.roleId,
                request.projectId,
                request.repoName!!
            )
        } else {
            roleRepository.findFirstByRoleIdAndProjectId(request.roleId, request.projectId)
        }

        role?.let {
            logger.warn("create role [${request.roleId} , ${request.projectId} ]  is exist.")
            return role.id
        }

        val result = roleRepository.insert(
            TRole(
                roleId = request.roleId,
                type = request.type,
                name = request.name,
                projectId = request.projectId,
                repoName = request.repoName,
                admin = request.admin
            )
        )
        return result.id
    }

    override fun detail(id: String): Role? {
        logger.info("get role detail : [$id] ")
        val result = roleRepository.findFirstById(id) ?: return null
        return transfer(result)
    }

    override fun detail(rid: String, projectId: String): Role? {
        logger.info("get  role  detail rid : [$rid] , projectId : [$projectId] ")
        val result = roleRepository.findFirstByRoleIdAndProjectId(rid, projectId) ?: return null
        return transfer(result)
    }

    override fun detail(rid: String, projectId: String, repoName: String): Role? {
        logger.info("get  role  detail rid : [$rid] , projectId : [$projectId], repoName: [$repoName]")
        val result = roleRepository.findFirstByRoleIdAndProjectIdAndRepoName(rid, projectId, repoName) ?: return null
        return transfer(result)
    }

    override fun listRoleByProject(type: RoleType?, projectId: String?, repoName: String?): List<Role> {
        logger.info("list  role  type : [$type] , projectId : [$projectId], repoName: [$repoName]")
        if (type == null && projectId == null) {
            return roleRepository.findAll().map { transfer(it) }
        } else if (type != null && projectId == null) {
            return roleRepository.findByType(type).map { transfer(it) }
        } else if (type == null && projectId != null) {
            return roleRepository.findByProjectId(projectId).map { transfer(it) }
        } else if (type != null && projectId != null) {
            return roleRepository.findByTypeAndProjectId(type, projectId).map { transfer(it) }
        } else if (projectId != null && repoName != null) {
            roleRepository.findByRepoNameAndProjectId(repoName, projectId).map { transfer(it) }
        }
        return emptyList()
    }

    override fun deleteRoleByid(id: String): Boolean {
        logger.info("delete  role  id : [$id]")
        roleRepository.findFirstById(id) ?: run {
            logger.warn("delete role [$id ] not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_ROLE_NOT_EXIST)
        }

        roleRepository.deleteById(id)
        return true
    }

    private fun transfer(tRole: TRole): Role {
        return Role(
            id = tRole.id,
            roleId = tRole.roleId,
            type = tRole.type,
            name = tRole.name,
            projectId = tRole.projectId,
            admin = tRole.admin
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RoleServiceImpl::class.java)
    }
}

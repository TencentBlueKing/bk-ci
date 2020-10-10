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

import com.mongodb.BasicDBObject
import com.tencent.bkrepo.auth.constant.DEFAULT_PASSWORD
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TUser
import com.tencent.bkrepo.auth.pojo.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.CreateUserToProjectRequest
import com.tencent.bkrepo.auth.pojo.Token
import com.tencent.bkrepo.auth.pojo.UpdateUserRequest
import com.tencent.bkrepo.auth.pojo.User
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.auth.util.DataDigestUtils
import com.tencent.bkrepo.auth.util.IDUtil
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "local")
class UserServiceImpl @Autowired constructor(
    private val userRepository: UserRepository,
    roleRepository: RoleRepository,
    private val mongoTemplate: MongoTemplate
) : UserService, AbstractServiceImpl(mongoTemplate, userRepository, roleRepository) {

    override fun createUser(request: CreateUserRequest): Boolean {
        // todo 校验
        logger.info("create user request : [$request]")
        val user = userRepository.findFirstByUserId(request.userId)
        user?.let {
            logger.warn("create user [${request.userId}]  is exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_DUP_UID)
        }
        if (request.group && request.asstUsers.isEmpty()) {
            throw ErrorCodeException(AuthMessageCode.AUTH_ASST_USER_EMPTY)
        }
        var pwd: String = DataDigestUtils.md5FromStr(DEFAULT_PASSWORD)
        request.pwd?.let {
            pwd = DataDigestUtils.md5FromStr(request.pwd!!)
        }
        userRepository.insert(
            TUser(
                userId = request.userId,
                name = request.name,
                pwd = pwd,
                admin = request.admin,
                locked = false,
                tokens = emptyList(),
                roles = emptyList(),
                asstUsers = request.asstUsers,
                group = request.group
            )
        )
        return true
    }

    override fun createUserToProject(request: CreateUserToProjectRequest): Boolean {
        // todo 校验
        logger.info("create user to project request : [$request]")

        val query = Query()
        query.addCriteria(Criteria.where("name").`is`(request.projectId))
        val result = mongoTemplate.count(query, "project")
        if (result == 0L) {
            logger.warn("user [${request.projectId}]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_PROJECT_NOT_EXIST)
        }
        // user not exist, create user
        val userResult = createUser(convCreateUserRequest(request))
        if (!userResult) {
            logger.warn("create user fail [$userResult]")
            return false
        }

        return true
    }

    override fun listUser(rids: List<String>): List<User> {
        logger.info("list user rids : [$rids]")
        return if (rids.isEmpty()) {
            userRepository.findAll().map { transferUser(it) }
        } else {
            userRepository.findAllByRolesIn(rids).map { transferUser(it) }
        }
    }

    override fun deleteById(userId: String): Boolean {
        logger.info("delete user userId : [$userId]")
        checkUserExist(userId)
        userRepository.deleteByUserId(userId)
        return true
    }

    override fun addUserToRole(userId: String, roleId: String): User? {
        logger.info("add user to role userId : [$userId], roleId : [$roleId]")
        // check user
        checkUserExist(userId)
        // check role
        checkRoleExist(roleId)
        val query = Query()
        val update = Update()
        query.addCriteria(Criteria.where(TUser::userId.name).`is`(userId))
        update.addToSet(TUser::roles.name, roleId)
        mongoTemplate.upsert(query, update, TUser::class.java)
        return getUserById(userId)
    }

    override fun addUserToRoleBatch(idList: List<String>, roleId: String): Boolean {
        logger.info("add user to role batch userId : [$idList], roleId : [$roleId]")
        checkUserExistBatch(idList)
        checkRoleExist(roleId)
        val query = Query()
        val update = Update()
        query.addCriteria(Criteria.where(TUser::userId.name).`in`(idList))
        update.addToSet(TUser::roles.name, roleId)
        mongoTemplate.updateMulti(query, update, TUser::class.java)
        return true
    }

    override fun removeUserFromRole(userId: String, roleId: String): User? {
        logger.info("remove user from role userId : [$userId], roleId : [$roleId]")
        // check user
        checkUserExist(userId)
        // check role
        checkRoleExist(roleId)
        val query = Query()
        val update = Update()
        query.addCriteria(Criteria.where(TUser::userId.name).`is`(userId).and(TUser::roles.name).`is`(roleId))
        update.unset("roles.$")
        mongoTemplate.upsert(query, update, TUser::class.java)
        return getUserById(userId)
    }

    override fun removeUserFromRoleBatch(idList: List<String>, roleId: String): Boolean {
        logger.info("remove user from role  batch userId : [$idList], roleId : [$roleId]")
        checkUserExistBatch(idList)
        checkRoleExist(roleId)
        val query = Query()
        val update = Update()
        query.addCriteria(Criteria.where(TUser::userId.name).`in`(idList).and(TUser::roles.name).`is`(roleId))
        update.unset("roles.$")
        val result = mongoTemplate.updateMulti(query, update, TUser::class.java)
        if (result.modifiedCount == 1L) return true
        return false
    }

    override fun updateUserById(userId: String, request: UpdateUserRequest): Boolean {
        logger.info("update user userId : [$userId], request : [$request]")
        checkUserExist(userId)

        val query = Query()
        query.addCriteria(Criteria.where(TUser::userId.name).`is`(userId))
        val update = Update()
        request.pwd?.let {
            val pwd = DataDigestUtils.md5FromStr(request.pwd!!)
            update.set(TUser::pwd.name, pwd)
        }
        request.admin?.let {
            update.set(TUser::admin.name, request.admin!!)
        }
        request.name?.let {
            update.set(TUser::name.name, request.name!!)
        }
        val result = mongoTemplate.updateFirst(query, update, TUser::class.java)
        if (result.modifiedCount == 1L) return true
        return false
    }

    override fun createToken(userId: String): User? {
        logger.info("create token userId : [$userId]")
        val token = IDUtil.genRandomId()
        return addUserToken(userId, token)
    }

    override fun addUserToken(userId: String, token: String): User? {
        logger.info("add user token userId : [$userId] ,token : [$token]")
        checkUserExist(userId)
        val query = Query.query(Criteria.where(TUser::userId.name).`is`(userId))
        val update = Update()
        val userToken = Token(id = token, createdAt = LocalDateTime.now(), expiredAt = LocalDateTime.now().plusYears(2))
        update.addToSet(TUser::tokens.name, userToken)
        mongoTemplate.upsert(query, update, TUser::class.java)
        return getUserById(userId)
    }

    override fun removeToken(userId: String, token: String): User? {
        logger.info("remove token userId : [$userId] ,token : [$token]")
        checkUserExist(userId)
        val query = Query.query(Criteria.where(TUser::userId.name).`is`(userId))
        val s = BasicDBObject()
        s["id"] = token
        val update = Update()
        update.pull(TUser::tokens.name, s)
        mongoTemplate.updateFirst(query, update, TUser::class.java)
        return getUserById(userId)
    }

    override fun getUserById(userId: String): User? {
        logger.info("get user userId : [$userId]")
        val user = userRepository.findFirstByUserId(userId) ?: return null
        return transferUser(user)
    }

    override fun findUserByUserToken(userId: String, pwd: String): User? {
        logger.info("find user userId : [$userId], pwd : [$pwd]")
        val hashPwd = DataDigestUtils.md5FromStr(pwd)
        val criteria = Criteria()
        criteria.orOperator(Criteria.where(TUser::pwd.name).`is`(hashPwd), Criteria.where("tokens.id").`is`(pwd))
            .and(TUser::userId.name).`is`(userId)
        val query = Query.query(criteria)
        val result = mongoTemplate.findOne(query, TUser::class.java) ?: return null
        return transferUser(result)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)
    }
}

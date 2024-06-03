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

package com.tencent.devops.auth.service

import com.tencent.devops.auth.constant.AuthI18nConstants.BK_AUTHORIZATION_SUCCEEDED
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_CANCELLED_AUTHORIZATION_SUCCEEDED
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.ManagerUserDao
import com.tencent.devops.auth.dao.ManagerUserHistoryDao
import com.tencent.devops.auth.dao.ManagerWhiteDao
import com.tencent.devops.auth.entity.UserChangeType
import com.tencent.devops.auth.pojo.ManagerUserEntity
import com.tencent.devops.auth.pojo.WhiteEntify
import com.tencent.devops.auth.pojo.dto.ManagerUserDTO
import com.tencent.devops.auth.pojo.enum.UrlType
import com.tencent.devops.auth.refresh.dispatch.AuthRefreshDispatch
import com.tencent.devops.auth.refresh.event.ManagerUserChangeEvent
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@Suppress("ALL")
@Service
class ManagerUserService @Autowired constructor(
    val dslContext: DSLContext,
    val managerUserDao: ManagerUserDao,
    val managerUserHistoryDao: ManagerUserHistoryDao,
    val managerWhiteDao: ManagerWhiteDao,
    val refreshDispatch: AuthRefreshDispatch,
    val managerOrganizationService: ManagerOrganizationService
) {

    @Value("\${devopsGateway.host:#{null}}")
    private val devopsGateway: String? = null

    fun batchCreateManagerByUser(userId: String, managerUser: ManagerUserDTO): Int {
        val users = managerUser.userId.split(",")
        users.forEach {
            val managerInfo = ManagerUserDTO(
                timeout = managerUser.timeout,
                userId = it,
                managerId = managerUser.managerId
            )
            createManagerUser(userId, managerInfo)
        }
        return users.count()
    }

    fun batchCreateManager(userId: String, managerId: String, managerUser: String, timeout: Int): Boolean {
        val managerIds = managerId.split(",")
        val managerUsers = managerUser.split(",")
        managerUsers.forEach { user ->
            managerIds.forEach { manager ->
                val managerInfo = ManagerUserDTO(
                    timeout = timeout,
                    userId = user,
                    managerId = manager.toInt()
                )
                createManagerUser(userId, managerInfo)
            }
        }
        return true
    }

    fun batchDelete(userId: String, managerIds: String, deleteUsers: String): Boolean {
        val managerIdArr = managerIds.split(",")
        val managerUsers = deleteUsers.split(",")
        managerUsers.forEach { user ->
            managerIdArr.forEach { manager ->
                deleteManagerUser(userId, manager.toInt(), user)
            }
        }
        return true
    }

    fun createManagerUser(userId: String, managerUser: ManagerUserDTO): Int {
        val managerInfo = ManagerUserEntity(
            createUser = userId,
            managerId = managerUser.managerId,
            startTime = System.currentTimeMillis(),
            timeoutTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(managerUser.timeout!!.toLong()),
            userId = managerUser.userId
        )
        LOG.info("createManagerUser | $managerInfo")

        val record = managerUserDao.get(dslContext, managerInfo.managerId, managerInfo.userId)

        if (record != null) {
            LOG.warn("createManagerUser user has this manager $userId $managerInfo $record")
            return 0
        }

        val id = managerUserDao.create(dslContext, managerInfo)
        managerUserHistoryDao.create(dslContext, managerInfo)
        LOG.info("createManagerUser send message to mq| $id | $userId | $managerUser")
        refreshDispatch.dispatch(
            ManagerUserChangeEvent(
                refreshType = "createManagerUser",
                userId = managerUser.userId,
                userChangeType = UserChangeType.CREATE,
                managerId = managerUser.managerId
            )
        )
        return id
    }

    fun deleteManagerUser(userId: String, managerId: Int, deleteUser: String): Boolean {
        LOG.info("deleteManagerUser | $userId | $deleteUser| $managerId")
        managerUserDao.delete(dslContext, managerId, deleteUser)
        val userHistoryRecords = managerUserHistoryDao.get(dslContext, managerId, deleteUser)
        if (userHistoryRecords == null) {
            LOG.info("deleteManagerUser history table is empty $managerId $deleteUser")
            return true
        }
        managerUserHistoryDao.updateById(
            dslContext = dslContext,
            id = userHistoryRecords.id,
            userId = userId
        )
        LOG.info("deleteManagerUser send message to mq | $userId | $managerId | $deleteUser")
        refreshDispatch.dispatch(
            ManagerUserChangeEvent(
                refreshType = "deleteManagerUser",
                userId = deleteUser,
                userChangeType = UserChangeType.DELETE,
                managerId = managerId
            )
        )
        return true
    }

    fun aliveManagerListByManagerId(managerId: Int): List<ManagerUserEntity>? {
        val watcher = Watcher("aliveManagerListByManagerId| $managerId")
        val managerList = mutableListOf<ManagerUserEntity>()

        try {
            val userRecords = managerUserDao.list(dslContext, managerId) ?: return null

            userRecords.forEach {
                val manager = ManagerUserEntity(
                    userId = it.userId,
                    managerId = it.managerId,
                    createUser = it.createUser,
                    timeoutTime = DateTimeUtil.convertLocalDateTimeToTimestamp(it.endTime),
                    startTime = DateTimeUtil.convertLocalDateTimeToTimestamp(it.startTime)
                )
                managerList.add(manager)
            }
            return managerList
        } catch (e: Exception) {
            LOG.warn("aliveManagerListByManagerId fail：", e)
            throw e
        } finally {
            LogUtils.printCostTimeWE(watcher, warnThreshold = 10, errorThreshold = 50)
        }
    }

    fun timeoutManagerListByManagerId(managerId: Int, page: Int? = 0, pageSize: Int? = 50): Page<ManagerUserEntity>? {
        val managerList = mutableListOf<ManagerUserEntity>()
        val watcher = Watcher("timeoutManagerListByManagerId| $managerId")
        try {
            val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)

            val userRecords = managerUserHistoryDao.list(dslContext, managerId, sqlLimit.limit, sqlLimit.offset)
                ?: return null
            val count = managerUserHistoryDao.count(dslContext, managerId)

            userRecords.forEach {
                val manager = ManagerUserEntity(
                    userId = it.userId,
                    managerId = it.managerId,
                    createUser = it.createUser,
                    timeoutTime = DateTimeUtil.convertLocalDateTimeToTimestamp(it.endTime),
                    startTime = DateTimeUtil.convertLocalDateTimeToTimestamp(it.startTime)
                )
                managerList.add(manager)
            }
            return Page(
                count = count.toLong(),
                page = page!!,
                pageSize = sqlLimit.limit,
                records = managerList
            )
        } catch (e: Exception) {
            LOG.warn("timeoutManagerListByManagerId fail:", e)
            throw e
        } finally {
            LogUtils.printCostTimeWE(watcher, warnThreshold = 10, errorThreshold = 50)
        }
    }

    fun getUserManagerIds(userId: String): List<String>? {
        val managerUserRecords = managerUserDao.getByUser(dslContext, userId)
        if (managerUserRecords == null || managerUserRecords.isEmpty()) {
            return emptyList()
        }
        val managerIds = mutableListOf<String>()
        managerUserRecords.forEach {
            managerIds.add(it.managerId.toString())
        }
        return managerIds
    }

    fun deleteTimeoutUser(): Boolean {
        var offset = 0
        val limit = 100
        LOG.info("auto delete timeoutUser start ${LocalTime.now()}")
        val watcher = Watcher("autoDeleteManager")
        try {
            do {
                watcher.start("$offset")
                val records = managerUserDao.timeoutList(dslContext, limit, offset)
                records?.forEach {
                    val userId = it.userId
                    val managerId = it.managerId
                    deleteManagerUser("system", managerId, userId)
                }
                offset += limit
            } while (records?.size == offset)
        } catch (e: Exception) {
            LOG.warn("auto delete TimeoutUser fail:", e)
        } finally {
            LogUtils.printCostTimeWE(watcher)
        }
        LOG.info("auto delete timeoutUser success")
        return true
    }

    fun createManagerUserByUrl(managerId: Int, userId: String): String {
        val whiteRecord = getWhiteUser(managerId, userId)
        if (whiteRecord == null) {
            LOG.warn("createManagerUserByUrl user:$userId not in $managerId whiteList")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.MANAGER_GRANT_WHITELIST_USER_EXIST
            )
        }
        val managerUser = ManagerUserDTO(
            managerId = managerId,
            userId = userId,
            timeout = 120
        )
        createManagerUser("system", managerUser)
        return MessageUtil.getMessageByLocale(BK_AUTHORIZATION_SUCCEEDED, I18nUtil.getLanguage(userId))
    }

    fun grantCancelManagerUserByUrl(managerId: Int, userId: String): String {
        val whiteRecord = getWhiteUser(managerId, userId)
        if (whiteRecord == null) {
            LOG.warn("grantCancelManagerUserByUrl user:$userId not in $managerId whiteList")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.MANAGER_GRANT_WHITELIST_USER_EXIST
            )
        }
        deleteManagerUser("system", managerId, userId)
        return MessageUtil.getMessageByLocale(BK_CANCELLED_AUTHORIZATION_SUCCEEDED, I18nUtil.getLanguage(userId))
    }

    fun createWhiteUser(managerId: Int, userIds: String): Boolean {
        val userList = userIds.split(",")
        dslContext.transaction { t ->
            val context = DSL.using(t)
            userList.forEach {
                if (it.isEmpty()) {
                    return@forEach
                }

                val record = managerWhiteDao.get(context, managerId, it)
                if (record != null) {
                    LOG.warn("createWhiteUser $managerId $it is exist")
                    throw ErrorCodeException(
                        errorCode = AuthMessageCode.MANAGER_WHITE_USER_EXIST
                    )
                }

                managerWhiteDao.create(context, managerId, it)
            }
        }
        return true
    }

    fun deleteWhiteUser(ids: String): Boolean {
        val idList = ids.split(",")
        dslContext.transaction { t ->
            val context = DSL.using(t)
            idList.forEach {
                val id = it.toInt()
                managerWhiteDao.delete(context, id)
            }
        }
        return true
    }

    fun listWhiteUser(managerId: Int): List<WhiteEntify>? {
        val records = managerWhiteDao.list(dslContext, managerId) ?: return emptyList()
        val whiteUsers = mutableListOf<WhiteEntify>()
        records.forEach {
            whiteUsers.add(WhiteEntify(
                id = it.id,
                managerId = it.managerId,
                user = it.userId
            ))
        }
        return whiteUsers
    }

    fun getWhiteUser(managerId: Int, userId: String): WhiteEntify? {
        val record = managerWhiteDao.get(dslContext, managerId, userId) ?: return null
        return WhiteEntify(
            id = record.id,
            managerId = record.managerId,
            user = record.userId
        )
    }

    fun getManagerUrl(managerId: Int, urlType: UrlType): String {
        val managerRecord = managerOrganizationService.getManagerInfo(managerId)
        if (managerRecord == null) {
            LOG.warn("getManagerUrl $managerId $urlType manager not exist")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.MANAGER_ORG_NOT_EXIST
            )
        }

        val host = devopsGateway
        return when (urlType) {
            UrlType.GRANT -> "$host/ms/auth/api/user/auth/manager/users/grant/$managerId"
            UrlType.CANCEL -> "$host/ms/auth/api/user/auth/manager/users/cancel/grant/$managerId"
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ManagerUserService::class.java)
    }
}

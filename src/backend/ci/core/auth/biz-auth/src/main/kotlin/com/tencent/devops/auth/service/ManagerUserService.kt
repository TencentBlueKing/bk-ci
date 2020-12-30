package com.tencent.devops.auth.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.ManagerUserDao
import com.tencent.devops.auth.dao.ManagerUserHistoryDao
import com.tencent.devops.auth.entity.UserChangeType
import com.tencent.devops.auth.pojo.ManagerUserEntity
import com.tencent.devops.auth.pojo.dto.ManagerUserDTO
import com.tencent.devops.auth.refresh.dispatch.AuthRefreshDispatch
import com.tencent.devops.auth.refresh.event.ManagerUserChangeEvent
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.service.utils.LogUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalTime

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
class ManagerUserService @Autowired constructor(
    val dslContext: DSLContext,
    val managerUserDao: ManagerUserDao,
    val managerUserHistoryDao: ManagerUserHistoryDao,
    val refreshDispatch: AuthRefreshDispatch
) {

    fun createManagerUser(userId: String, managerUser: ManagerUserDTO): Int {
        logger.info("createManagerUser | $userId | $managerUser")
        val managerInfo = ManagerUserEntity(
            createUser = userId,
            managerId = managerUser.managerId,
            startTime = System.currentTimeMillis(),
            timeoutTime = System.currentTimeMillis() + (DateTimeUtil.minuteToSecond(managerUser.timeout!!) * 1000),
            userId = managerUser.userId
        )

        val record = managerUserDao.get(dslContext, managerInfo.managerId, managerInfo.userId)

        if (record != null) {
            logger.warn("createManagerUser user has this manager $userId $managerInfo $record")
            throw ErrorCodeException(
                defaultMessage = "",
                errorCode = AuthMessageCode.MANAGER_USER_EXIST
            )
        }

        val id = managerUserDao.create(dslContext, managerInfo)
        managerUserHistoryDao.create(dslContext, managerInfo)
        logger.info("createManagerUser send message to mq| $id | $userId | $managerUser")
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
        logger.info("deleteManagerUser | $userId | $deleteUser")
        logger.info("deleteManagerUser delete alive table $deleteUser, $managerId")
        val id = managerUserDao.delete(dslContext, managerId, deleteUser)
        logger.info("deleteManagerUser update history table $deleteUser, $managerId")
        val userHistoryRecords = managerUserHistoryDao.get(dslContext, managerId, deleteUser)
        if (userHistoryRecords == null) {
            logger.info("deleteManagerUser history table is empty $managerId $deleteUser")
            return true
        }
        managerUserHistoryDao.updateById(
            dslContext = dslContext,
            id = userHistoryRecords.id,
            userId = userId
        )
        logger.info("deleteManagerUser send message to mq | $userId | $managerId | $deleteUser")
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
            logger.warn("aliveManagerListByManagerId fail：", e)
            throw e
        } finally {
            LogUtils.printCostTimeWE(watcher, warnThreshold = 10, errorThreshold = 50)
        }
    }

    fun timeoutManagerListByManagerId(managerId: Int, page: Int ? = 0, pageSize: Int ? = 50): Page<ManagerUserEntity>? {
        val managerList = mutableListOf<ManagerUserEntity>()
        val watcher = Watcher("timeoutManagerListByManagerId| $managerId")
        try {
            val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)

            val userRecords = managerUserHistoryDao.list(dslContext, managerId, sqlLimit.limit, sqlLimit.offset) ?: return null
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
            logger.warn("timeoutManagerListByManagerId fail:", e)
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
        logger.info("auto delete timeoutUser start ${LocalTime.now()}")
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
            logger.warn("auto delete TimeoutUser fail:", e)
        } finally {
            LogUtils.printCostTimeWE(watcher)
        }
        logger.info("auto delete timeoutUser success")
        return true
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}

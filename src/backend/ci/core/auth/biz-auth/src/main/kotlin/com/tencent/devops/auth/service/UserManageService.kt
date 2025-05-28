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

import com.tencent.devops.auth.common.Constants
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.DepartmentDao
import com.tencent.devops.auth.dao.DepartmentRelationDao
import com.tencent.devops.auth.dao.UserInfoDao
import com.tencent.devops.auth.entity.SearchUserAndDeptEntity
import com.tencent.devops.auth.pojo.BkUserDeptInfo
import com.tencent.devops.auth.pojo.DepartmentInfo
import com.tencent.devops.auth.pojo.DepartmentUserCount
import com.tencent.devops.auth.pojo.UserInfo
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.RetryUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class UserManageService @Autowired constructor(
    val dslContext: DSLContext,
    val userInfoDao: UserInfoDao,
    val departmentDao: DepartmentDao,
    val departmentRelationDao: DepartmentRelationDao,
    val deptService: DeptService,
    val client: Client
) {
    @Value("\${esb.code:#{null}}")
    val appCode: String = ""

    @Value("\${esb.secret:#{null}}")
    val appSecret: String = ""

    fun getUserDepartmentDistribution(
        userIds: List<String>,
        parentId: Int
    ): List<DepartmentUserCount> {
        val childrenIds = departmentDao.listDepartmentChildren(
            dslContext = dslContext,
            parentId = parentId
        )
        return departmentDao.getUserCountOfDepartments(
            dslContext = dslContext,
            userIds = userIds,
            departmentIds = childrenIds
        )
    }

    fun getUserInfo(userId: String): UserInfo {
        return userInfoDao.get(
            dslContext = dslContext,
            userId = userId
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.USER_NOT_EXIST,
            params = arrayOf(userId),
            defaultMessage = "user $userId not exist"
        )
    }

    fun syncUserInfoData() {
        Executors.newFixedThreadPool(1).execute {
            val startEpoch = System.currentTimeMillis()
            var page = 1
            val pageSize = PageUtil.MAX_PAGE_SIZE
            logger.info("start to sync user info data")
            val previousUserSyncDataRecord = userInfoDao.getLatestUserSyncDataRecord(
                dslContext = dslContext,
                taskType = USER_SYNC_TASK_TYPE
            )
            val latestTaskId = UUIDUtil.generate()
            userInfoDao.recordSyncDataTask(
                dslContext = dslContext,
                taskId = latestTaskId,
                taskType = USER_SYNC_TASK_TYPE
            )
            // 同步用户数据
            do {
                val bkUserInfos = try {
                    RetryUtils.retryAnyException(retryTime = 3, retryPeriodMills = 100) {
                        deptService.listUserInfos(
                            searchUserEntity = SearchUserAndDeptEntity(
                                lookupField = Constants.USERNAME,
                                bk_app_code = appCode,
                                bk_app_secret = appSecret,
                                fields = Constants.USER_LABEL,
                                page = page,
                                pageSize = pageSize
                            )
                        ).results
                    }
                } catch (ex: Exception) {
                    logger.warn("list User Infos failed $page|$pageSize|$ex")
                    emptyList()
                }
                bkUserInfos.forEach { bkUserInfo ->
                    logger.info("sync user info data ,{}", bkUserInfo)
                    try {
                        val deptInfoDTO = extractDeptInfo(bkUserInfo.userName)
                        userInfoDao.create(
                            dslContext = dslContext,
                            userInfo = UserInfo(
                                userId = bkUserInfo.userName,
                                userName = bkUserInfo.displayName,
                                enabled = bkUserInfo.enabled ?: true,
                                departmentName = deptInfoDTO?.departmentName,
                                departmentId = deptInfoDTO?.departmentId,
                                departments = deptInfoDTO?.departments,
                                path = deptInfoDTO?.path,
                                departed = false
                            ),
                            taskId = latestTaskId
                        )
                    } catch (ex: Exception) {
                        logger.warn("sync user info data failed $bkUserInfo|$ex")
                    }
                }
                page += 1
            } while (bkUserInfos.size == pageSize)
            // 标记用户是否离职
            previousUserSyncDataRecord.takeIf { it != null }?.let {
                userInfoDao.updateUserDepartedFlag(
                    dslContext = dslContext,
                    taskId = it.taskId
                )
            }
            userInfoDao.recordSyncDataTask(
                dslContext = dslContext,
                taskId = latestTaskId,
                taskType = USER_SYNC_TASK_TYPE
            )
            logger.info("It take(${System.currentTimeMillis() - startEpoch})ms to sync user info data")
        }
    }

    private fun extractDeptInfo(userName: String): DeptInfoDTO? {
        val userDeptDetails = deptService.getUserDeptDetails(userId = userName)
        return when {
            userDeptDetails == null -> {
                null
            }

            userDeptDetails.family.isNullOrEmpty() -> {
                DeptInfoDTO(
                    departmentName = userDeptDetails.name,
                    departmentId = userDeptDetails.id,
                    path = mutableListOf(userDeptDetails.id),
                    departments = mutableListOf(
                        BkUserDeptInfo(
                            id = userDeptDetails.id.toString(),
                            name = userDeptDetails.name,
                            fullName = userDeptDetails.name
                        )
                    )
                )
            }

            else -> {
                val departments = userDeptDetails.family!!.toMutableList().apply {
                    add(
                        BkUserDeptInfo(
                            id = userDeptDetails.id.toString(),
                            name = userDeptDetails.name,
                            fullName = userDeptDetails.family!!.last().fullName?.plus("/${userDeptDetails.name}")
                        )
                    )
                }
                val path = departments.map { it.id!!.toInt() }.toMutableList()
                DeptInfoDTO(
                    departmentName = userDeptDetails.name,
                    departmentId = userDeptDetails.id,
                    path = path,
                    departments = departments
                )
            }
        }
    }

    // 数据类用于封装部门信息
    private data class DeptInfoDTO(
        val departmentName: String,
        val departmentId: Int,
        val path: MutableList<Int>,
        val departments: MutableList<BkUserDeptInfo>
    )

    fun syncDepartmentInfoData() {
        Executors.newFixedThreadPool(1).execute {
            val startEpoch = System.currentTimeMillis()
            var page = 1
            val pageSize = PageUtil.MAX_PAGE_SIZE
            logger.info("start to sync department info data")
            val previousDepartmentSyncDataRecord = userInfoDao.getLatestUserSyncDataRecord(
                dslContext = dslContext,
                taskType = DEPARTMENT_SYNC_TASK_TYPE
            )
            val latestTaskId = UUIDUtil.generate()
            userInfoDao.recordSyncDataTask(
                dslContext = dslContext,
                taskId = latestTaskId,
                taskType = DEPARTMENT_SYNC_TASK_TYPE
            )
            do {
                val deptInfos = try {
                    RetryUtils.retryAnyException(retryTime = 3, retryPeriodMills = 100) {
                        deptService.listDeptInfos(
                            searchUserEntity = SearchUserAndDeptEntity(
                                bk_app_code = appCode,
                                bk_app_secret = appSecret,
                                page = page,
                                pageSize = pageSize
                            )
                        ).results
                    }
                } catch (ex: Exception) {
                    logger.warn("list dept infos failed $page|$pageSize|$ex")
                    emptyList()
                }
                deptInfos.forEach { deptInfo ->
                    logger.info("sync department info data {}", deptInfo)
                    departmentDao.create(
                        dslContext = dslContext,
                        departmentInfo = DepartmentInfo(
                            departmentId = deptInfo.id,
                            departmentName = deptInfo.name,
                            parent = deptInfo.parent,
                            level = deptInfo.level,
                            hasChildren = deptInfo.hasChildren
                        ),
                        taskId = latestTaskId
                    )
                }
                page += 1
            } while (deptInfos.size == pageSize)
            previousDepartmentSyncDataRecord.takeIf { it != null }?.let {
                departmentDao.deleteByTaskId(
                    dslContext = dslContext,
                    taskId = it.taskId
                )
            }
            syncDepartmentRelations()
            userInfoDao.recordSyncDataTask(
                dslContext = dslContext,
                taskId = latestTaskId,
                taskType = DEPARTMENT_SYNC_TASK_TYPE
            )
            logger.info("It take(${System.currentTimeMillis() - startEpoch})ms to sync department info data")
        }
    }

    fun syncDepartmentRelations() {
        val startEpoch = System.currentTimeMillis()
        var offset = 0
        val limit = PageUtil.MAX_PAGE_SIZE
        logger.info("start to sync department relations data")
        do {
            val departments = departmentDao.list(dslContext, limit, offset)
            departments.forEach { dept ->
                logger.info("sync department relations data,{$dept}")
                // 获取当前部门的所有祖先（包括自身）
                val ancestors = getDepartmentParents(dept.departmentId)
                ancestors.forEach { (parentId, parentLevel) ->
                    // 计算深度 = 当前部门层级 - 祖先部门层级
                    val depth = dept.level - parentLevel
                    departmentRelationDao.create(dslContext, parentId, dept.departmentId, depth)
                }
            }
            offset += limit
        } while (departments.size == limit)
        logger.info("It take(${System.currentTimeMillis() - startEpoch})ms to sync department relations data")
    }

    fun getDepartmentParents(departmentId: Int): List<Pair<Int/*parentId*/, Int/*level*/>> {
        val chain = mutableListOf<Pair<Int, Int>>()
        var currentDept = departmentDao.get(dslContext, departmentId)
        while (currentDept != null) {
            chain.add(currentDept.departmentId to (currentDept.level))
            currentDept = departmentDao.get(dslContext, currentDept.parent)
        }
        return chain
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserManageService::class.java)
        private const val USER_SYNC_TASK_TYPE = "USER"
        private const val DEPARTMENT_SYNC_TASK_TYPE = "DEPARTMENT"
    }
}

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

package com.tencent.devops.project.service

import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.model.project.tables.records.TUserRecord
import com.tencent.devops.project.dao.ProjectFreshDao
import com.tencent.devops.project.dao.ProjectUserDao
import com.tencent.devops.project.dao.UserDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
@Suppress("ALL")
class ProjectUserRefreshService @Autowired constructor(
    val tofService: TOFService,
    val projectUserService: ProjectUserService,
    val userDao: UserDao,
    val projectUserDao: ProjectUserDao,
    val projectFreshDao: ProjectFreshDao,
    val projectDispatcher: ProjectDispatcher,
    val dslContext: DSLContext
) {
    private val executorService = Executors.newSingleThreadExecutor()

    fun refreshUser(userId: String): UserDeptDetail? {
        val userRecord = projectUserService.getUserDept(userId)
        return if (userRecord == null) {
            logger.info("user is empty, add userInfo from tof")
            createUser(userId)
        } else {
            synUserInfo(userRecord, userId)
        }
    }

    fun refreshAllUser(): Boolean {
        executorService.execute {
            val startTime = System.currentTimeMillis()
            // 开始同步数据
            var page = 1
            val pageSize = 1000
            var continueFlag = true
            while (continueFlag) {
                val pageLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
                logger.info("refreshAllUser page: $page , pageSize: $pageSize, " +
                    "limit: ${pageLimit.limit}, offset: ${pageLimit.offset}")
                val userList = projectUserService.listUser(pageLimit.limit, pageLimit.offset)
                if (userList == null) {
                    continueFlag = false
                    continue
                }
                updateInfoByTof(userList)

                if (userList.size < pageSize) {
                    continueFlag = false
                    continue
                }
                Thread.sleep(5000)
                page++
            }
            logger.info("Syn all userInfo ${System.currentTimeMillis() - startTime}ms")
        }
        return true
    }

    private fun updateInfoByTof(userInfo: List<TUserRecord>) {
        userInfo.forEach {
            try {
                Thread.sleep(5)
                val tofDeptInfo = tofService.getDeptFromTof(null, it.userId, "", false)
                if (tofDeptInfo.centerId.toInt() != it.centerId || tofDeptInfo.deptId.toInt() != it.deptId) {
                    logger.info("${it.userId} cent id is diff, " +
                        "tof ${tofDeptInfo.centerId} ${tofDeptInfo.centerName}, " +
                        "local ${it.centerId} ${it.centerName}")
                    userDao.update(
                        userId = it.userId,
                        groupId = tofDeptInfo.groupId.toInt(),
                        groupName = tofDeptInfo.groupName,
                        bgId = tofDeptInfo.bgId.toInt(),
                        bgName = tofDeptInfo.bgName,
                        centerId = tofDeptInfo.centerId.toInt(),
                        centerName = tofDeptInfo.centerName,
                        deptId = tofDeptInfo.deptId.toInt(),
                        deptName = tofDeptInfo.deptName,
                        dslContext = dslContext,
                        name = it.name
                    )
                }
            } catch (e: Exception) {
                logger.warn("updateInfoByTof ${it.userId} fail: $e")
            }
        }
    }

    // 添加用户
    fun createUser(userId: String): UserDeptDetail {
        // user表不存在，直接同步 数据源直接获取tof数据
        val tofDeptInfo = tofService.getDeptFromTof(null, userId, "", false)
        val staffInfo = tofService.getStaffInfo(userId)
        userDao.create(
            dslContext = dslContext,
            groupId = tofDeptInfo.groupId.toInt(),
            groupName = tofDeptInfo.groupName,
            bgId = tofDeptInfo.bgId.toInt(),
            bgName = tofDeptInfo.bgName,
            centerId = tofDeptInfo.deptId.toInt(),
            centerName = tofDeptInfo.deptName,
            deptId = tofDeptInfo.deptId.toInt(),
            deptName = tofDeptInfo.deptName,
            name = staffInfo.ChineseName,
            userId = userId
        )
        return tofDeptInfo
    }

    // 同步用户信息
    fun synUserInfo(userInfo: UserDeptDetail, userId: String): UserDeptDetail? {
        try {
            val staffInfo = tofService.getStaffInfo(userId)
            if (userInfo!!.groupId != staffInfo.GroupId) {
                logger.info("user info diff, bk:${userInfo.groupId}, tof :${staffInfo.GroupId}")
                // 组织信息不一致，刷新当前用户数据。 以tof数据为准, 数据源直接获取tof数据
                val tofDeptInfo = tofService.getDeptFromTof(null, userId, "", false)
                userDao.update(
                    userId = userId,
                    groupId = tofDeptInfo.groupId.toInt(),
                    groupName = tofDeptInfo.groupName,
                    bgId = tofDeptInfo.bgId.toInt(),
                    bgName = tofDeptInfo.bgName,
                    centerId = tofDeptInfo.centerId.toInt(),
                    centerName = tofDeptInfo.centerName,
                    deptId = tofDeptInfo.deptId.toInt(),
                    deptName = tofDeptInfo.deptName,
                    dslContext = dslContext,
                    name = staffInfo.ChineseName
                )
                return tofDeptInfo
            }
        } catch (e: OperationException) {
            // 删除已离职用户
            projectUserDao.delete(dslContext, userId)
        }

        return null
    }

    fun resetProjectInfo(): Int {
        return projectFreshDao.resetProjectDeptInfo(dslContext)
    }

    fun fixGitCIProjectInfo(): Int {
        val limitCount = 5
        var count = 0
        var startId = 0L
        var currProjects = projectFreshDao.getProjectAfterId(dslContext, startId, limitCount)
        while (currProjects.isNotEmpty()) {
            currProjects.forEach {
                try {
                    val devopsUser = projectFreshDao.getDevopsUserInfo(dslContext, it.creator)
                    if (devopsUser == null) {
                        val userInfo = tofService.getUserDeptDetail(it.creator)
                        logger.info("[${it.creator}] fixGitCIProjectInfo tofService: $userInfo")
                        count += projectFreshDao.fixProjectInfo(
                            dslContext = dslContext,
                            id = it.id,
                            creatorBgId = userInfo.bgId.toLong(),
                            creatorBgName = userInfo.bgName,
                            creatorDeptId = userInfo.deptId.toLong(),
                            creatorDeptName = userInfo.deptName,
                            creatorCenterId = userInfo.centerId.toLong(),
                            creatorCenterName = userInfo.centerName
                        )
                        projectDispatcher.dispatch(
                            ProjectUpdateBroadCastEvent(
                                userId = it.creator,
                                projectId = it.projectId,
                                projectInfo = ProjectUpdateInfo(
                                    projectName = it.projectName,
                                    projectType = it.projectType,
                                    bgId = userInfo.bgId.toLong(),
                                    bgName = userInfo.bgName,
                                    centerId = userInfo.centerId.toLong(),
                                    centerName = userInfo.centerName,
                                    deptId = userInfo.deptId.toLong(),
                                    deptName = userInfo.deptName,
                                    description = it.description,
                                    englishName = it.englishName,
                                    ccAppId = it.ccAppId,
                                    ccAppName = it.ccAppName,
                                    kind = it.kind,
                                    secrecy = it.isSecrecy
                                )
                            )
                        )
                    } else {
                        logger.info("[${it.creator}] fixGitCIProjectInfo getDevopsUserInfo: ${devopsUser.toJsonString()}")
                        count += projectFreshDao.fixProjectInfo(
                            dslContext = dslContext,
                            id = it.id,
                            creatorBgId = devopsUser.bgId.toLong(),
                            creatorBgName = devopsUser.bgName,
                            creatorDeptId = devopsUser.deptId.toLong(),
                            creatorDeptName = devopsUser.deptName,
                            creatorCenterId = devopsUser.centerId.toLong(),
                            creatorCenterName = devopsUser.centerName
                        )
                        projectDispatcher.dispatch(
                            ProjectUpdateBroadCastEvent(
                                userId = it.creator,
                                projectId = it.projectId,
                                projectInfo = ProjectUpdateInfo(
                                    projectName = it.projectName,
                                    projectType = it.projectType,
                                    bgId = devopsUser.bgId.toLong(),
                                    bgName = devopsUser.bgName,
                                    centerId = devopsUser.centerId.toLong(),
                                    centerName = devopsUser.centerName,
                                    deptId = devopsUser.deptId.toLong(),
                                    deptName = devopsUser.deptName,
                                    description = it.description,
                                    englishName = it.englishName,
                                    ccAppId = it.ccAppId,
                                    ccAppName = it.ccAppName,
                                    kind = it.kind,
                                    secrecy = it.isSecrecy
                                )
                            )
                        )
                    }
                } catch (t: Throwable) {
                    logger.error("Update git ci project in devops failed, msg: ${t.message}")
                    return@forEach
                } finally {
                    startId = it.id
                }
            }
            logger.info("fixGitCIProjectInfo project ${currProjects.map { it.id }.toList()}, fixed count: $count")
            Thread.sleep(100)
            currProjects = projectFreshDao.getProjectAfterId(dslContext, startId, limitCount)
        }
        logger.info("fixGitCIProjectInfo finished count: $count")
        return count
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectUserRefreshService::class.java)
    }
}

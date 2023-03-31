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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_IS_INVALID
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.project.tables.records.TUserRecord
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.dao.ProjectFreshDao
import com.tencent.devops.project.dao.ProjectUserDao
import com.tencent.devops.project.dao.UserDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.UserInfo
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
                logger.info(
                    "refreshAllUser page: $page , pageSize: $pageSize, " +
                        "limit: ${pageLimit.limit}, offset: ${pageLimit.offset}"
                )
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

    private fun updateInfoByTof(userInfoList: List<UserDeptDetail>) {
        userInfoList.forEach {
            try {
                Thread.sleep(5)
                try {
                    val tofDeptInfo = tofService.getDeptFromTof(null, it.userId!!, "", false)
                    if (tofDeptInfo == null) {
                        projectUserDao.delete(dslContext, it.userId!!)
                        logger.info("user ${it.userId} is level office, delete t_user info")
                    } else if (
                        isUserInfoChange(
                            tofDeptInfo = tofDeptInfo,
                            dbUserRecord = it
                        )
                    ) {
                        logger.info(
                            "${it.userId} cent id is diff, " +
                                "tof ${tofDeptInfo.centerId} ${tofDeptInfo.centerName}, " +
                                "local ${it.centerId} ${it.centerName}"
                        )
                        userDao.update(
                            userId = it.userId!!,
                            groupId = tofDeptInfo.groupId.toInt(),
                            groupName = tofDeptInfo.groupName,
                            bgId = tofDeptInfo.bgId.toInt(),
                            bgName = tofDeptInfo.bgName,
                            centerId = tofDeptInfo.centerId.toInt(),
                            centerName = tofDeptInfo.centerName,
                            deptId = tofDeptInfo.deptId.toInt(),
                            deptName = tofDeptInfo.deptName,
                            dslContext = dslContext,
                            name = it.name!!
                        )
                    }
                } catch (oe: OperationException) {
                    logger.warn("getUserDept fail: ${it.userId}|$oe")
                }
            } catch (e: Exception) {
                logger.warn("updateInfoByTof ${it.userId} fail: $e")
            }
        }
    }

    // 添加用户
    fun createUser(userId: String): UserDeptDetail? {
        // user表不存在，直接同步 数据源直接获取tof数据
        val tofDeptInfo = tofService.getDeptFromTof(null, userId, "", false)
        if (tofDeptInfo == null) {
            logger.warn("creatUser $userId tofInfo is level office. cancel create")
            return null
        }
        val staffInfo = tofService.getStaffInfo(userId)
        userDao.create(
            dslContext = dslContext,
            groupId = tofDeptInfo.groupId.toInt(),
            groupName = tofDeptInfo.groupName,
            bgId = tofDeptInfo.bgId.toInt(),
            bgName = tofDeptInfo.bgName,
            centerId = tofDeptInfo.centerId.toInt(),
            centerName = tofDeptInfo.centerName,
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
            val deptInfo = tofService.getDeptFromTof(
                operator = null,
                userId = userId,
                bkTicket = "",
                userCache = false
            ) ?: throw OperationException(I18nUtil.getCodeLanMessage(
                messageCode = "user $userId level office",
                language = I18nUtil.getLanguage(userId)
            ))
            if (isUserInfoChange(
                    tofDeptInfo = deptInfo,
                    dbUserRecord = userInfo
                )) {
                logger.info("user info diff, bk:$userInfo, tof :$deptInfo")
                // 组织信息不一致，刷新当前用户数据。 以tof数据为准, 数据源直接获取tof数据
                projectUserDao.update(
                    userId = userId,
                    groupId = deptInfo.groupId.toInt(),
                    groupName = deptInfo.groupName,
                    bgId = deptInfo.bgId.toInt(),
                    bgName = deptInfo.bgName,
                    centerId = deptInfo.centerId.toInt(),
                    centerName = deptInfo.centerName,
                    deptId = deptInfo.deptId.toInt(),
                    deptName = deptInfo.deptName,
                    dslContext = dslContext
                )
                return deptInfo
            }
        } catch (e: OperationException) {
            logger.warn("user $userId is level office")
            // 删除已离职用户
            projectUserDao.delete(dslContext, userId)
        }

        return null
    }

    fun resetProjectInfo(): Int {
        return projectFreshDao.resetProjectDeptInfo(dslContext)
    }

    fun fixGitCIProjectInfo(start: Long, limitCount: Int, sleepTime: Long): Int {
        var startId = start
        var count = 0
        var currProjects = projectFreshDao.getProjectAfterId(dslContext, startId, limitCount)
        while (currProjects.isNotEmpty()) {
            currProjects.forEach {
                try {
                    val devopsUser = projectFreshDao.getDevopsUserInfo(dslContext, it.creator)
                    if (devopsUser == null) {
                        Thread.sleep(sleepTime)
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
                        logger.info(
                            "[${it.creator}] fixGitCIProjectInfo getDevopsUserInfo: " +
                                "creatorBgId=${devopsUser.bgId}, creatorBgName=${devopsUser.bgName}" +
                                "creatorDeptId=${devopsUser.deptId}, creatorDeptName=${devopsUser.deptName}" +
                                "creatorCenterId=${devopsUser.centerId}, creatorCenterName=${devopsUser.centerName}"
                        )
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
            currProjects = projectFreshDao.getProjectAfterId(dslContext, startId, limitCount)
        }
        logger.info("fixGitCIProjectInfo finished count: $count")
        return count
    }

    fun createPublicAccount(userInfo: UserInfo): Boolean {
        val userMessage = userDao.get(dslContext, userInfo.userId)
        if (userMessage != null) {
            logger.warn("createPublicAccount ${userInfo.userId} ${userInfo.name} is exist")
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(userInfo.userId)
            )
        }
        try {
            // 校验是否在rtx用户, 若为rtx用户不符合公共账号的判断逻辑
            tofService.getUserDeptDetail(userInfo.userId)
            logger.warn("createPublicAccount ${userInfo.userId} is not public account")
            throw ErrorCodeException(
                errorCode = PARAMETER_IS_INVALID,
                params = arrayOf(userInfo.userId)
            )
        } catch (e: OperationException) {
            logger.info("createPublicAccount ${userInfo.userId} is public account")
        }

        userDao.create(
            dslContext = dslContext,
            userId = userInfo.userId,
            name = userInfo.userId,
            bgId = userInfo.bgId,
            bgName = userInfo.bgName,
            deptId = userInfo.deptId ?: 0,
            deptName = userInfo.deptName ?: "",
            centerId = userInfo.centerId ?: 0,
            centerName = userInfo.centerName ?: "",
            groupId = userInfo.groupId ?: 0,
            groupName = userInfo.groupName ?: "",
            publicAccount = true
        )
        return true
    }

    private fun isUserInfoChange(
        tofDeptInfo: UserDeptDetail,
        dbUserRecord: UserDeptDetail
    ): Boolean {
        return tofDeptInfo.bgId != dbUserRecord.bgId ||
            tofDeptInfo.bgName != dbUserRecord.bgName ||
            tofDeptInfo.deptId != dbUserRecord.deptId ||
            tofDeptInfo.deptName != dbUserRecord.deptName ||
            tofDeptInfo.centerId != dbUserRecord.centerId ||
            tofDeptInfo.centerName != dbUserRecord.centerName ||
            tofDeptInfo.groupId != dbUserRecord.groupId ||
            tofDeptInfo.groupName != dbUserRecord.groupName
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectUserRefreshService::class.java)
    }
}

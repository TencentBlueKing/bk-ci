package com.tencent.devops.project.service

import com.tencent.devops.project.dao.UserDao
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectUserRefreshService @Autowired constructor(
    val tofService: TOFService,
    val projectUserService: ProjectUserService,
    val userDao: UserDao,
    val dslContext: DSLContext
) {
    fun refreshUser(userId: String): UserDeptDetail? {
        val userRecord = projectUserService.getUserDept(userId)
        return if (userRecord == null) {
            logger.info("user is empty, add userInfo from tof")
            createUser(userId)
        } else {
            synUserInfo(userRecord, userId)
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
                    centerId = tofDeptInfo.deptId.toInt(),
                    centerName = tofDeptInfo.deptName,
                    deptId = tofDeptInfo.deptId.toInt(),
                    deptName = tofDeptInfo.deptName,
                    dslContext = dslContext,
                    name = staffInfo.ChineseName
            )
            return tofDeptInfo
        }
        return null
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
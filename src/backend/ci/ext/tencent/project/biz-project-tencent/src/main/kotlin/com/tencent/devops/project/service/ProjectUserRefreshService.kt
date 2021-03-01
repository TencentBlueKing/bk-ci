package com.tencent.devops.project.service

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.model.project.tables.records.TUserRecord
import com.tencent.devops.project.dao.UserDao
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class ProjectUserRefreshService @Autowired constructor(
    val tofService: TOFService,
    val projectUserService: ProjectUserService,
    val userDao: UserDao,
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
            var page = 0
            val pageSize = 1000
            var continueFlag = true
            while (continueFlag) {
                val pageLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
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
            val tofDeptInfo = tofService.getDeptFromTof(null, it.userId, "", false)
            if (tofDeptInfo.centerId.toInt() != it.centerId) {
                logger.info("${it.userId} cent id is diff, " +
                    "tof ${tofDeptInfo.centerId} ${tofDeptInfo.centerName}, " +
                    "local ${it.centerId} ${it.centerName}")
            }
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
                centerId = tofDeptInfo.centerId.toInt(),
                centerName = tofDeptInfo.centerName,
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

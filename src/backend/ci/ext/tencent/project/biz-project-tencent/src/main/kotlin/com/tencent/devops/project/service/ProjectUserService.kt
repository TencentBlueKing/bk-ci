package com.tencent.devops.project.service

import com.tencent.devops.model.project.tables.records.TUserRecord
import com.tencent.devops.project.dao.ProjectUserDao
import com.tencent.devops.project.dao.UserDao
import com.tencent.devops.project.pojo.user.UserDeptDetail
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectUserService @Autowired constructor(
    val dslContext: DSLContext,
    val userDao: UserDao,
    val projectUserDao: ProjectUserDao
) {
    fun getUserDept(userId: String): UserDeptDetail? {
        val userRecord = userDao.get(dslContext, userId) ?: return null
        return UserDeptDetail(
                bgName = userRecord!!.bgName,
                bgId = userRecord!!.bgId?.toString() ?: "",
                centerName = userRecord.centerName,
                centerId = userRecord!!.centerId?.toString() ?: "",
                deptName = userRecord.deptName,
                deptId = userRecord.deptId?.toString() ?: "",
                groupName = userRecord.groupName,
                groupId = userRecord.groypId?.toString() ?: ""
        )
    }

    fun listUser(limit: Int, offset: Int): List<TUserRecord>? {
        val limitByMax = if (limit > 1000) {
            1000
        } else {
            limit
        }

        return projectUserDao.list(
            dslContext = dslContext,
            limit = limitByMax,
            offset = offset
        )
    }
}

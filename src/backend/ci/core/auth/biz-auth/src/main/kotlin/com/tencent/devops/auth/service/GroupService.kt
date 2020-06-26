package com.tencent.devops.auth.service

import com.tencent.devops.auth.dao.AuthGroupDao
import com.tencent.devops.auth.dao.AuthGroupPermissionDao
import com.tencent.devops.auth.dao.AuthGroupUserDao
import com.tencent.devops.auth.entity.GroupCreateInfo
import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.common.api.pojo.Result
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
class GroupService @Autowired constructor(
    val dslContext: DSLContext,
    val groupDao: AuthGroupDao,
    val groupUserDao: AuthGroupUserDao,
    val groupPermissionDao: AuthGroupPermissionDao
) {
    fun createGroup(
        userId: String,
        projectCode: String,
        addCreateUser: Boolean?,
        groupInfo: GroupDTO
    ): Result<String> {
        logger.info("createGroup |$userId|$projectCode|$addCreateUser|$groupInfo")
        val groupRecord = groupDao.getGroup(
            dslContext = dslContext,
            projectCode = projectCode,
            groupCode = groupInfo.groupCode
        )
        if(groupRecord != null) {
            // 项目下分组已存在,不能重复创建
        }
        val groupCreateInfo = GroupCreateInfo(
            groupCode = groupInfo.groupCode,
            groupType = groupInfo.groupType,
            groupName = groupInfo.groupName,
            projectCode = projectCode,
            user = userId
        )
        val groupId = groupDao.createGroup(dslContext, groupCreateInfo)

        // 若新建分组不是内置分组，需建立分组与权限关系
        if(groupInfo.groupType == 1) {
            if(groupInfo.authPermissionList == null || groupInfo.authPermissionList!!.isEmpty()) {
                // 自定义分组未选权限,抛异常
                throw RuntimeException()
            }
            // 建立用户组与权限关系
            groupPermissionDao.batchCreateAction(
                dslContext = dslContext,
                groupCode = groupCreateInfo.groupCode,
                userId = userId,
                authActions = groupInfo.authPermissionList!!
            )

        }

        // 若需要添加创建人到该分组
        if(addCreateUser != null && addCreateUser) {
            groupUserDao.create(dslContext, userId, groupInfo.groupCode)
        }

        return Result(groupId)
    }

    companion object{
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
package com.tencent.devops.auth.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthGroupDao
import com.tencent.devops.auth.dao.AuthGroupPermissionDao
import com.tencent.devops.auth.dao.AuthGroupUserDao
import com.tencent.devops.auth.entity.GroupCreateInfo
import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.auth.pojo.enum.GroupType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.auth.tables.records.TAuthGroupRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
class GroupService @Autowired constructor(
    val dslContext: DSLContext,
    val groupDao: AuthGroupDao,
    val groupPermissionDao: AuthGroupPermissionDao
) {
    fun createGroup(
        userId: String,
        projectCode: String,
        groupInfo: GroupDTO
    ): Result<String> {
        logger.info("createGroup |$userId|$projectCode||$groupInfo")
        val groupRecord = groupDao.getGroup(
            dslContext = dslContext,
            projectCode = projectCode,
            groupCode = groupInfo.groupCode
        )
        if(groupRecord != null) {
            // 项目下分组已存在,不能重复创建
            logger.warn("createGroup |$userId| $projectCode| $groupInfo is exsit")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(AuthMessageCode.GROUP_EXIST))
        }
        val groupCreateInfo = GroupCreateInfo(
            groupCode = groupInfo.groupCode,
            groupType = groupInfo.groupType.ordinal,
            groupName = groupInfo.groupName,
            projectCode = projectCode,
            user = userId
        )
        val groupId = groupDao.createGroup(dslContext, groupCreateInfo)

        // 若新建分组不是内置分组，需建立分组与权限关系
        if(groupInfo.groupType == GroupType.USER_BUILD) {
            if(groupInfo.authPermissionList == null || groupInfo.authPermissionList!!.isEmpty()) {
                logger.warn("createGroup group is not bind permission| $userId| $projectCode| $groupInfo")
                // 自定义分组未选权限,抛异常
                throw OperationException(MessageCodeUtil.getCodeLanMessage(AuthMessageCode.GROUP_NOT_BIND_PERSSION))
            }
            // 建立用户组与权限关系
            groupPermissionDao.batchCreateAction(
                dslContext = dslContext,
                groupCode = groupCreateInfo.groupCode,
                userId = userId,
                authActions = groupInfo.authPermissionList!!
            )
        }

        return Result(groupId)
    }

    fun getGroupCode(groupId: String) : TAuthGroupRecord? {
        return groupDao.getGroupById(dslContext, groupId)
    }

    companion object{
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
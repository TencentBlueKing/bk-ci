package com.tencent.devops.auth.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthGroupUserDao
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GroupUserService @Autowired constructor(
    val dslContext: DSLContext,
    val groupService: GroupService,
    val groupUserDao: AuthGroupUserDao
) {
    fun addUser2Group(userId: String, groupId: String): Result<Boolean> {
        logger.info("addUser2Group |$userId| $groupId")
        val groupUserRecord = groupUserDao.get(
            dslContext = dslContext,
            userId = userId,
            groupId = groupId
        )
        if(groupUserRecord != null) {
            logger.warn("addUser2Group user $userId already in this group $groupId")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(AuthMessageCode.GROUP_USER_ALREADY_EXIST))
        }
        val groupRecord = groupService.getGroupCode(groupId)

        if(groupRecord == null) {
            logger.warn("addUser2Group group $groupId is not exist")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(AuthMessageCode.GROUP_NOT_EXIST))
        }
        // 添加用户至用户组
        groupUserDao.create(
            dslContext = dslContext,
            userId = userId,
            groupId = groupId
        )
        return Result(true)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
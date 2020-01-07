package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.service.common.TxStoreGitResitoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxStoreGitResitoryServiceImpl @Autowired constructor(
    private val client: Client
) : TxStoreGitResitoryService {
    override fun addRepoMember(
        storeMemberReq: StoreMemberReq,
        userId: String,
        repositoryHashId: String
    ): Result<Boolean> {
        logger.info("addRepoMember storeMemberReq is:$storeMemberReq,userId is:$userId,repositoryHashId is:$repositoryHashId")
        if (repositoryHashId.isNotBlank()) {
            val gitAccessLevel = if (storeMemberReq.type == StoreMemberTypeEnum.ADMIN) GitAccessLevelEnum.MASTER else GitAccessLevelEnum.DEVELOPER
            val addGitProjectMemberResult = client.get(ServiceGitRepositoryResource::class)
                .addGitProjectMember(userId, storeMemberReq.member, repositoryHashId, gitAccessLevel, TokenTypeEnum.PRIVATE_KEY)
            logger.info("addGitProjectMemberResult is:$addGitProjectMemberResult")
            return addGitProjectMemberResult
        }
        return Result(true)
    }

    override fun deleteRepoMember(userId: String, username: String, repositoryHashId: String): Result<Boolean> {
        logger.info("deleteRepoMember userId is:$userId,username is:$username,repositoryHashId is:$repositoryHashId")
        if (repositoryHashId.isNotBlank()) {
            val deleteGitProjectMemberResult = client.get(ServiceGitRepositoryResource::class)
                .deleteGitProjectMember(userId, listOf(username), repositoryHashId, TokenTypeEnum.PRIVATE_KEY)
            logger.info("deleteGitProjectMemberResult is:$deleteGitProjectMemberResult")
            return deleteGitProjectMemberResult
        }
        return Result(true)
    }

    companion object{
        val logger = LoggerFactory.getLogger(TxStoreGitResitoryServiceImpl::class.java)
    }
}
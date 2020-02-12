package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.service.common.TxStoreGitResitoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxExtServiceMemberImpl @Autowired constructor(
    private val txStoreGitResitoryService: TxStoreGitResitoryService
) : ExtServiceMemberImpl() {

    override fun addRepoMember(
        storeMemberReq: StoreMemberReq,
        userId: String,
        repositoryHashId: String
    ): Result<Boolean> {
        return txStoreGitResitoryService.addRepoMember(storeMemberReq, userId, repositoryHashId)
    }

    override fun deleteRepoMember(userId: String, username: String, repositoryHashId: String): Result<Boolean> {
        return txStoreGitResitoryService.deleteRepoMember(userId, username, repositoryHashId)
    }
}
package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreMemberReq

interface TxStoreGitResitoryService {
    fun addRepoMember(storeMemberReq: StoreMemberReq, userId: String, repositoryHashId: String): Result<Boolean>

    fun deleteRepoMember(userId: String, username: String, repositoryHashId: String): Result<Boolean>
}
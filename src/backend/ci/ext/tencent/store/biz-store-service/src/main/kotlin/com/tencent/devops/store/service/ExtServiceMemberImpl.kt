package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.impl.StoreMemberServiceImpl

abstract class ExtServiceMemberImpl : StoreMemberServiceImpl() {
    override fun add(
        userId: String,
        storeMemberReq: StoreMemberReq,
        storeType: StoreTypeEnum,
        collaborationFlag: Boolean?,
        sendNotify: Boolean
    ): Result<Boolean> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    abstract fun addRepoMember(storeMemberReq: StoreMemberReq, userId: String, repositoryHashId: String): Result<Boolean>

    override fun delete(userId: String, id: String, storeCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    abstract fun deleteRepoMember(userId: String, username: String, repositoryHashId: String): Result<Boolean>

    override fun getStoreName(storeCode: String): String {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
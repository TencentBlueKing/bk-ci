package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.api.UserExtServiceMembersResource
import com.tencent.devops.store.pojo.common.StoreMemberItem
import com.tencent.devops.store.pojo.common.StoreMemberReq
import org.springframework.beans.factory.annotation.Autowired

class UserExtServiceMemberResourceImpl @Autowired constructor(

): UserExtServiceMembersResource {
    override fun list(userId: String, serviceCode: String): Result<List<StoreMemberItem?>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun add(userId: String, storeMemberReq: StoreMemberReq): Result<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(userId: String, id: String, serviceCode: String): Result<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun view(userId: String, serviceCode: String): Result<StoreMemberItem?> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun changeMemberTestProjectCode(
        accessToken: String,
        userId: String,
        projectCode: String,
        serviceCode: String
    ): Result<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
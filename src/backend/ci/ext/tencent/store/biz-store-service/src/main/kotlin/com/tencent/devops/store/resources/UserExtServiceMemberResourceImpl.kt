package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceMembersResource
import com.tencent.devops.store.pojo.common.StoreMemberItem
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.TxExtServiceMemberImpl
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceMemberResourceImpl @Autowired constructor(
    private val txExtServiceMemberImpl: TxExtServiceMemberImpl
) : UserExtServiceMembersResource {
    override fun list(userId: String, serviceCode: String): Result<List<StoreMemberItem?>> {
        return txExtServiceMemberImpl.list(
            userId = userId,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE
        )
    }

    override fun add(userId: String, storeMemberReq: StoreMemberReq): Result<Boolean> {
        return txExtServiceMemberImpl.add(
            userId = userId,
            storeMemberReq = storeMemberReq,
            sendNotify = true,
            storeType = StoreTypeEnum.SERVICE,
            collaborationFlag = true

        )
    }

    override fun delete(userId: String, id: String, serviceCode: String): Result<Boolean> {
        return txExtServiceMemberImpl.delete(
            userId = userId,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE,
            id = id
            )
    }

    override fun view(userId: String, serviceCode: String): Result<StoreMemberItem?> {
        return txExtServiceMemberImpl.viewMemberInfo(
            userId = userId,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE
            )
    }

    override fun changeMemberTestProjectCode(
        accessToken: String,
        userId: String,
        projectCode: String,
        serviceCode: String
    ): Result<Boolean> {
        return txExtServiceMemberImpl.changeMemberTestProjectCode(
            accessToken = accessToken,
            userId = userId,
            projectCode = projectCode,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE
        )
    }
}
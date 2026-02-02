package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.store.common.service.StoreManagementExtraService
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("TRIGGER_EVENT_MANAGEMENT_EXTRA_SERVICE")
class TriggerEventManagementExtraService : StoreManagementExtraService {
    override fun doComponentDeleteCheck(storeCode: String): Result<Boolean> {
        logger.info("doComponentDeleteCheck")
        return Result(true)
    }

    override fun deleteComponentRepoFile(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<Boolean> {
        logger.info("deleteComponentRepoFile")
        return Result(true)
    }

    override fun deleteComponentCodeRepository(
        userId: String,
        repositoryId: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        logger.info("deleteComponentCodeRepository")
        return Result(true)
    }

    override fun addComponentRepositoryUser(
        memberType: StoreMemberTypeEnum,
        members: List<String>,
        repositoryId: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        logger.info("addComponentRepositoryUser")
        return Result(true)
    }

    override fun deleteComponentRepositoryUser(
        member: String,
        repositoryId: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean> {
        logger.info("deleteComponentRepositoryUser")
        return Result(true)
    }

    override fun uninstallComponentCheck(
        userId: String,
        projectCode: String,
        storeType: String,
        storeCode: String
    ): Result<Boolean> {
        logger.info("uninstallComponentCheck")
        return Result(true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerEventManagementExtraService::class.java)
    }
}
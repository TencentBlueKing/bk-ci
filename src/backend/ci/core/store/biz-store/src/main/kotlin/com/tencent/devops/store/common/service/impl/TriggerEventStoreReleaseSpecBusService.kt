package com.tencent.devops.store.common.service.impl

import com.tencent.devops.store.common.service.StoreReleaseSpecBusService
import com.tencent.devops.store.pojo.common.QueryComponentPkgEnvInfoParam
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.publication.StoreCreateRequest
import com.tencent.devops.store.pojo.common.publication.StorePkgEnvInfo
import com.tencent.devops.store.pojo.common.publication.StoreRunPipelineParam
import com.tencent.devops.store.pojo.common.publication.StoreUpdateRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("TRIGGER_EVENT_RELEASE_SPEC_BUS_SERVICE")
class TriggerEventStoreReleaseSpecBusService : StoreReleaseSpecBusService {
    override fun doStoreCreatePreBus(storeCreateRequest: StoreCreateRequest) {
        logger.info("doStoreCreatePreBus")
    }

    override fun doStoreUpdatePreBus(storeUpdateRequest: StoreUpdateRequest) {
        logger.info("doStoreUpdatePreBus")
    }

    override fun doStoreI18nConversionSpecBus(storeUpdateRequest: StoreUpdateRequest) {
        logger.info("storeUpdateRequest")
    }

    override fun doCheckStoreUpdateParamSpecBus(storeUpdateRequest: StoreUpdateRequest) {
        logger.info("doCheckStoreUpdateParamSpecBus")
    }

    override fun getStoreUpdateStatus(): StoreStatusEnum {
        return StoreStatusEnum.INIT
    }

    override fun getStoreRunPipelineStartParams(
        storeRunPipelineParam: StoreRunPipelineParam
    ): MutableMap<String, String> {
        return mutableMapOf()
    }

    override fun getStoreRunPipelineStatus(buildId: String?, startFlag: Boolean): StoreStatusEnum? {
        logger.info("getStoreRunPipelineStatus")
        return null
    }

    override fun getComponentPkgEnvInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        osName: String?,
        osArch: String?
    ): List<StorePkgEnvInfo> {
        logger.info("getComponentPkgEnvInfo")
        return listOf()
    }

    override fun getComponentPkgEnvInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        queryComponentPkgEnvInfoParam: QueryComponentPkgEnvInfoParam
    ): List<StorePkgEnvInfo> {
        logger.info("getComponentPkgEnvInfo")
        return listOf()
    }

    override fun getReleaseProcessItems(
        userId: String,
        isNormalUpgrade: Boolean,
        status: StoreStatusEnum
    ): List<ReleaseProcessItem> {
        logger.info("getReleaseProcessItems")
        return listOf()
    }

    override fun doStoreEnvBus(
        storeCode: String,
        storeType: StoreTypeEnum,
        version: String,
        userId: String,
        releaseType: ReleaseTypeEnum?
    ) {
        logger.info("doStoreEnvBus")
    }

    override fun doStorePostCreateBus(userId: String, storeCode: String, storeType: StoreTypeEnum) {
        logger.info("doStorePostCreateBus")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerEventStoreReleaseSpecBusService::class.java)
    }
}
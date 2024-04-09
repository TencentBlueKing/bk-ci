package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.pojo.AtomBaseInfo
import com.tencent.devops.store.common.service.StoreSpecBusService
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.publication.StorePkgEnvInfo
import com.tencent.devops.store.pojo.common.publication.StoreRunPipelineParam
import com.tencent.devops.store.pojo.common.publication.StoreUpdateRequest

class StoreSpecBusServiceImpl : StoreSpecBusService {
    override fun doStoreI18nConversionSpecBus(storeUpdateRequest: StoreUpdateRequest) {
        TODO("Not yet implemented")
    }

    override fun doCheckStoreUpdateParamSpecBus(storeUpdateRequest: StoreUpdateRequest) {
        TODO("Not yet implemented")
    }

    override fun getStoreUpdateStatus(): StoreStatusEnum {
        TODO("Not yet implemented")
    }

    override fun getStoreRunPipelineStartParams(storeRunPipelineParam: StoreRunPipelineParam): MutableMap<String, String> {
        TODO("Not yet implemented")
    }

    override fun getStoreRunPipelineStatus(buildId: String?): StoreStatusEnum? {
        TODO("Not yet implemented")
    }

    override fun getComponentPkgEnvInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String
    ): List<StorePkgEnvInfo> {
        TODO("Not yet implemented")
    }

    override fun getReleaseProcessItems(
        userId: String,
        isNormalUpgrade: Boolean,
        status: StoreStatusEnum
    ): List<ReleaseProcessItem> {
        TODO("Not yet implemented")
    }

    override fun doComponentDeleteCheck(): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun deleteComponentRepoFile(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun uninstallComponentParamCheck(
        userId: String,
        projectCode: String,
        storeType: String,
        storeCode: String
    ): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun getProcessingVersionInfo(
        userId: String,
        storeCodeList: List<String>
    ): Map<String, List<AtomBaseInfo>>? {
        TODO("Not yet implemented")
    }
}
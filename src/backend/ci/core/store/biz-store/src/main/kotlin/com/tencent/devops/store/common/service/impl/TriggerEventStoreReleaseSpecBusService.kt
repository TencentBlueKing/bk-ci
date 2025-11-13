package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.BEGIN
import com.tencent.devops.common.api.constant.BUILD
import com.tencent.devops.common.api.constant.COMMIT
import com.tencent.devops.common.api.constant.TEST
import com.tencent.devops.common.web.utils.I18nUtil
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
        // TODO: 定义发布流程
//        val processInfo = mutableListOf<ReleaseProcessItem>()
//        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = BEGIN), BEGIN, NUM_ONE, SUCCESS))
//        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = COMMIT), COMMIT, NUM_TWO, UNDO))
//        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = BUILD), BUILD, NUM_THREE, UNDO))
//        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = TEST), TEST, NUM_FOUR, UNDO))
//        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = EDIT), EDIT, NUM_FIVE, UNDO))
//        if (isNormalUpgrade) {
//            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = END), END, NUM_SIX, UNDO))
//        } else {
//            processInfo.add(
//                ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = APPROVE), APPROVE, NUM_SIX, UNDO)
//            )
//            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = END), END, NUM_SEVEN, UNDO))
//        }
//        val totalStep = if (isNormalUpgrade) NUM_SIX else NUM_SEVEN
//        when (status) {
//            StoreStatusEnum.INIT, StoreStatusEnum.COMMITTING -> {
//                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_TWO, DOING)
//            }
//
//            StoreStatusEnum.BUILDING -> {
//                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, DOING)
//            }
//
//            StoreStatusEnum.BUILD_FAIL -> {
//                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, FAIL)
//            }
//
//            StoreStatusEnum.TESTING -> {
//                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FOUR, DOING)
//            }
//
//            StoreStatusEnum.EDITING -> {
//                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, DOING)
//            }
//
//            StoreStatusEnum.AUDITING -> {
//                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_SIX, DOING)
//            }
//
//            StoreStatusEnum.AUDIT_REJECT -> {
//                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_SIX, FAIL)
//            }
//
//            StoreStatusEnum.RELEASED -> {
//                val currStep = if (isNormalUpgrade) NUM_SIX else NUM_SEVEN
//                storeCommonService.setProcessInfo(processInfo, totalStep, currStep, SUCCESS)
//            }
//
//            else -> {}
//        }
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

    override fun doStoreCreatePostBus(userId: String, storeCode: String, storeType: StoreTypeEnum) {
        logger.info("doStoreCreatePostBus")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerEventStoreReleaseSpecBusService::class.java)
    }
}
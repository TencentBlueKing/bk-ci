package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.APPROVE
import com.tencent.devops.common.api.constant.BEGIN
import com.tencent.devops.common.api.constant.BUILD
import com.tencent.devops.common.api.constant.COMMIT
import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.EDIT
import com.tencent.devops.common.api.constant.END
import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.NUM_FIVE
import com.tencent.devops.common.api.constant.NUM_FOUR
import com.tencent.devops.common.api.constant.NUM_ONE
import com.tencent.devops.common.api.constant.NUM_SEVEN
import com.tencent.devops.common.api.constant.NUM_SIX
import com.tencent.devops.common.api.constant.NUM_THREE
import com.tencent.devops.common.api.constant.NUM_TWO
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.constant.TEST
import com.tencent.devops.common.api.constant.UNDO
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreReleaseSpecBusService
import com.tencent.devops.store.event.utils.TriggerEventConverter
import com.tencent.devops.store.pojo.common.KEY_ATOM_FORM
import com.tencent.devops.store.pojo.common.KEY_TRIGGER_EVENT_CONFIG
import com.tencent.devops.store.pojo.common.QueryComponentPkgEnvInfoParam
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.publication.StoreCreateRequest
import com.tencent.devops.store.pojo.common.publication.StorePkgEnvInfo
import com.tencent.devops.store.pojo.common.publication.StoreRunPipelineParam
import com.tencent.devops.store.pojo.common.publication.StoreUpdateRequest
import com.tencent.devops.store.pojo.event.TriggerEventConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("TRIGGER_EVENT_RELEASE_SPEC_BUS_SERVICE")
@SuppressWarnings("TooManyFunctions")
class TriggerEventReleaseSpecBusService @Autowired constructor(
    private val storeCommonService: StoreCommonService
): StoreReleaseSpecBusService {
    override fun doStoreCreatePreBus(storeCreateRequest: StoreCreateRequest) {
        logger.info("doStoreCreatePreBus")
        storeCreateRequest.baseInfo.extBaseInfo?.let {
            // 根据入参生成触发器task.json
            val extBaseParam = generateAtomForm(it)
            it.putAll(extBaseParam)
        }
    }

    override fun doStoreUpdatePreBus(storeUpdateRequest: StoreUpdateRequest) {
        storeUpdateRequest.baseInfo.extBaseInfo?.let {
            // 根据入参生成触发器task.json
            val extBaseParam = generateAtomForm(it)
            it.putAll(extBaseParam)
        }
    }

    override fun doStoreI18nConversionSpecBus(storeUpdateRequest: StoreUpdateRequest) {
        logger.info("storeUpdateRequest")
    }

    override fun doCheckStoreUpdateParamSpecBus(storeUpdateRequest: StoreUpdateRequest) {
        logger.info("doCheckStoreUpdateParamSpecBus")
    }

    override fun getStoreUpdateStatus(): StoreStatusEnum {
        return StoreStatusEnum.COMMITTING
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
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(COMMIT, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(BUILD, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(TEST, NUM_FOUR, UNDO))
        processInfo.add(ReleaseProcessItem(EDIT, NUM_FIVE, UNDO))
        if (isNormalUpgrade) {
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = END), END, NUM_SIX, UNDO))
        } else {
            processInfo.add(
                ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = APPROVE), APPROVE, NUM_SIX, UNDO)
            )
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = END), END, NUM_SEVEN, UNDO))
        }
        val totalStep = if (isNormalUpgrade) NUM_SIX else NUM_SEVEN
        when (status) {
            StoreStatusEnum.INIT, StoreStatusEnum.COMMITTING -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_TWO, DOING)
            }

            StoreStatusEnum.BUILDING -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, DOING)
            }

            StoreStatusEnum.BUILD_FAIL -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, FAIL)
            }

            StoreStatusEnum.TESTING -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FOUR, DOING)
            }

            StoreStatusEnum.EDITING -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, DOING)
            }

            StoreStatusEnum.AUDITING -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_SIX, DOING)
            }

            StoreStatusEnum.AUDIT_REJECT -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_SIX, FAIL)
            }

            StoreStatusEnum.RELEASED -> {
                val currStep = if (isNormalUpgrade) NUM_SIX else NUM_SEVEN
                storeCommonService.setProcessInfo(processInfo, totalStep, currStep, SUCCESS)
            }

            else -> {}
        }
        return processInfo
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

    /**
     * 根据触发事件配置，生成对应的触发器表单配置
     */
    private fun generateAtomForm(extBaseInfo: Map<String, Any>?): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        extBaseInfo?.get(KEY_TRIGGER_EVENT_CONFIG)?.let {
            try {
                when (it) {
                    is String -> {
                        JsonUtil.to(it, TriggerEventConfig::class.java)
                    }

                    is Map<*, *> -> {
                        JsonUtil.mapTo(it as Map<String, Any>, TriggerEventConfig::class.java)
                    }

                    else -> {
                        logger.warn("trigger event config[$it] is not string or map")
                        null
                    }
                }
            } catch (ignored: Exception) {
                logger.warn("fail to convert trigger event config[$it]", ignored)
                null
            }?.let { eventConfig ->
                val atomForm = TriggerEventConverter.convertAtomForm(eventConfig)
                map[KEY_ATOM_FORM] = JsonUtil.toJson(atomForm)
            }
        }
        return map
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerEventReleaseSpecBusService::class.java)
    }
}
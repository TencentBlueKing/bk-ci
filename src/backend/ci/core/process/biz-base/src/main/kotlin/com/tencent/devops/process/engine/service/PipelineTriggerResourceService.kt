package com.tencent.devops.process.engine.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.store.api.common.ServiceStoreComponentResource
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线触发资源服务
 */
@Service
class PipelineTriggerResourceService @Autowired constructor(
    val client: Client
) {
    fun getTriggerEventInfo(list: Collection<BuildInfo>): Map<String, String> {
        val storeCodes = list.filter {
            it.trigger == StartType.TRIGGER_EVENT.name
        }.mapNotNull {
            it.triggerEventType
        }.filter {
            it.isNotBlank()
        }.toSet()
        return try {
            client.get(ServiceStoreComponentResource::class).getComponentBaseInfoByCodes(
                storeType = StoreTypeEnum.TRIGGER_EVENT,
                storeCodes = storeCodes.joinToString(separator = ",")
            ).data?.associate {
                it.storeCode to it.storeName
            } ?: mapOf()
        } catch (ignored: Exception) {
            logger.warn("fail to get trigger event info", ignored)
            mapOf()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTriggerResourceService::class.java)
    }
}
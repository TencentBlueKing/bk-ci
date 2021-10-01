package com.tencent.devops.auth.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthPipelineIdService @Autowired constructor(
    val client: Client
) {
    fun findPipelineAutoId(resourceType: String, resourceCode: String): String {
        // 非pipeline的资源类型直接返回
        if (resourceType != AuthResourceType.PIPELINE_DEFAULT.value) {
            return resourceCode
        }
        return try {
            // 若不能转成Int,则为pipelineId,需要做转换
            resourceCode.toInt()
            resourceCode
        } catch (e: Exception) {
            logger.info("pipeline check permission user pipelineId $resourceCode")
            val pipelineInfo = client.get(ServicePipelineResource::class)
                .getPipelineInfoByPipelineId(resourceCode)?.data
                ?: throw PermissionForbiddenException(
                    message = MessageCodeUtil.getCodeMessage(
                        messageCode = CommonMessageCode.PERMISSION_DENIED,
                        params = arrayOf(resourceCode))
                )
            pipelineInfo!!.id.toString()
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AuthPipelineIdService::class.java)
    }
}

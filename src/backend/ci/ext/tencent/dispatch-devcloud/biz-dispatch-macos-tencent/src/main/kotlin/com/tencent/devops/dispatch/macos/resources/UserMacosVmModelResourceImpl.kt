package com.tencent.devops.dispatch.macos.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.macos.api.UserMacosVmModelResource
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmModelData
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmModelRequest
import com.tencent.devops.dispatch.macos.service.DevCloudMacosService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * User接口 - macOS VM Model资源实现
 */
@RestResource
class UserMacosVmModelResourceImpl @Autowired constructor(
    private val devCloudMacosService: DevCloudMacosService
) : UserMacosVmModelResource {

    companion object {
        private val logger = LoggerFactory.getLogger(UserMacosVmModelResourceImpl::class.java)
    }

    override fun getVmModelList(request: DevCloudMacosVmModelRequest): Result<List<DevCloudMacosVmModelData>> {
        logger.info("Get VM model list with request: platform=${request.platform}, t1=${request.t1}, t2=${request.t2}")
        
        val response = devCloudMacosService.getVmModel(request, "")
        
        if (response == null) {
            logger.error("Failed to get VM model list from DevCloud")
            return Result(emptyList())
        }
        
        if (response.actionCode != 200) {
            logger.error("Get VM model list failed, actionCode: ${response.actionCode}, message: ${response.actionMessage}")
            return Result(emptyList())
        }
        
        return Result(response.data ?: emptyList())
    }
}

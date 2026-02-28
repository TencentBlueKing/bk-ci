package com.tencent.devops.dispatch.macos.resources

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.macos.api.UserMacosVmModelResource
import com.tencent.devops.dispatch.macos.pojo.devcloud.DMAllVmModelData
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmModelData
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmModelImage
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmModelRequest
import com.tencent.devops.dispatch.macos.service.DevCloudMacosService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

/**
 * User接口 - macOS VM Model资源实现
 */
@RestResource
class UserMacosVmModelResourceImpl @Autowired constructor(
    private val devCloudMacosService: DevCloudMacosService
) : UserMacosVmModelResource {

    companion object {
        private val logger = LoggerFactory.getLogger(UserMacosVmModelResourceImpl::class.java)

        // getAllVmModels 结果缓存，有效期1天
        private const val ALL_VM_MODELS_CACHE_KEY = "all_vm_models"
        private val allVmModelsCache = Caffeine.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build<String, DMAllVmModelData>()

        // 默认VM Model数据，当DevCloud服务不可用时返回
        private val DEFAULT_VM_MODEL_LIST = listOf(
            DevCloudMacosVmModelData(
                name = "VMware苹果",
                uid = "VMware",
                desc = "VMware苹果",
                enabled = true,
                images = listOf(
                    DevCloudMacosVmModelImage(
                        name = "10.14.4",
                        uid = "10.14.4",
                        desc = "10.14.4",
                        xcodes = listOf("10.0", "10.1", "10.2.1", "11.0", "11.1", "11.2", "11.2.1", "11.3", "11.3.1")
                    ),
                    DevCloudMacosVmModelImage(
                        name = "Catalina10.15.4",
                        uid = "Catalina10.15.4",
                        desc = "Catalina10.15.4",
                        xcodes = listOf(
                            "10.0", "10.1", "10.2.1", "11.0", "11.1", "11.2", "11.2.1", "11.3", "11.3.1",
                            "11.4", "11.4.1", "11.5", "11.6", "12", "12.1", "12.2", "12.3", "12.4", "12.5"
                        )
                    ),
                    DevCloudMacosVmModelImage(
                        name = "BigSur11.4",
                        uid = "BigSur11.4",
                        desc = "BigSur11.4",
                        xcodes = listOf(
                            "11.2", "11.2.1", "11.3", "11.3.1", "11.4", "11.4.1", "11.5", "11.6",
                            "12", "12.1", "12.2", "12.3", "12.4", "12.5", "13", "13.1", "13.2", "13.2.1", "14"
                        )
                    ),
                    DevCloudMacosVmModelImage(
                        name = "Monterey12.4",
                        uid = "Monterey12.4",
                        desc = "Monterey12.4",
                        xcodes = listOf(
                            "13.3", "13.3.1", "13.4", "13.4.1", "13.2.1", "13.2", "13.1", "13",
                            "14", "14.0.1", "14.1", "14.2"
                        )
                    ),
                    DevCloudMacosVmModelImage(
                        name = "Ventura",
                        uid = "Ventura",
                        desc = "Ventura",
                        xcodes = listOf("14.3", "14.3.1", "15", "15.0.1", "15.1", "15.2")
                    ),
                    DevCloudMacosVmModelImage(
                        name = "Sonoma",
                        uid = "Sonoma",
                        desc = "Sonoma",
                        xcodes = listOf("15", "15.0.1", "15.1", "15.2", "15.3", "15.4", "16", "16.1", "16.2")
                    )
                )
            )
        )
    }

    override fun getVmModelList(request: DevCloudMacosVmModelRequest): Result<List<DevCloudMacosVmModelData>> {
        logger.info("Get VM model list with request: platform=${request.platform}, t1=${request.t1}, t2=${request.t2}")
        
        val response = devCloudMacosService.getVmModel(request, "")
        
        if (response == null || response.actionCode != 200) {
            logger.error("Failed to get VM model list from DevCloud, return default VM model list")

            // 返回默认值
            return Result(DEFAULT_VM_MODEL_LIST)
        }
        
        return Result(response.data ?: DEFAULT_VM_MODEL_LIST)
    }

    override fun getAllVmModels(): Result<DMAllVmModelData> {
        // 先从缓存获取
        val cachedData = allVmModelsCache.getIfPresent(ALL_VM_MODELS_CACHE_KEY)
        if (cachedData != null) {
            logger.info("Get all VM models from cache")
            return Result(cachedData)
        }

        val response = devCloudMacosService.getAllVmModels("")

        if (response == null) {
            logger.error("Failed to getAllVmModes from DevCloud")
            return Result(DMAllVmModelData(emptyList(), emptyList()))
        }

        if (response.actionCode != 200) {
            logger.error("GetAllVmModes failed, actionCode: ${response.actionCode}, message: ${response.actionMessage}")
            return Result(DMAllVmModelData(emptyList(), emptyList()))
        }

        val data = response.data ?: DMAllVmModelData(emptyList(), emptyList())
        // 缓存结果
        allVmModelsCache.put(ALL_VM_MODELS_CACHE_KEY, data)
        logger.info("Cache all VM models data successfully")

        return Result(data)
    }
}
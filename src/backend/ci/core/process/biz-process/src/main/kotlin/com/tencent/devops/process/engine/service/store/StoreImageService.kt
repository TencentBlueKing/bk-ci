package com.tencent.devops.process.engine.service.store

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.client.Client
import com.tencent.devops.store.api.image.service.ServiceStoreImageResource
import com.tencent.devops.store.constant.StoreMessageCode.USER_IMAGE_NOT_INSTALLED
import com.tencent.devops.store.pojo.image.exception.ImageNotInstalledException
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * @Description
 * @Date 2019/10/17
 * @Version 1.0
 */

@Service
class StoreImageService @Autowired constructor(
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StoreImageService::class.java)
    }

    // 从镜像商店获取完整的镜像名称
    fun getImageRepoInfo(
        userId: String,
        projectId: String,
        imageCode: String?,
        imageVersion: String?,
        defaultPrefix: String?
    ): ImageRepoInfo {
        logger.info("getImageRepoInfo:Input($userId,$projectId,$imageCode,$imageVersion)")
        // 鉴权：安装了才有权限使用
        if (null == imageCode) {
            throw InvalidParamException("Input:($userId,$projectId,$imageCode),imageCode is null")
        }
        val permissionResult = client.get(ServiceStoreImageResource::class).isInstalled(userId, projectId, imageCode)
        if (permissionResult.isNotOk() || (!permissionResult.data!!)) {
            throw ImageNotInstalledException("Input:($userId,$projectId,$imageCode)", USER_IMAGE_NOT_INSTALLED)
        }
        // 调商店接口获取镜像信息
        val result = client.get(ServiceStoreImageResource::class)
            .getImageRepoInfoByCodeAndVersion(
                userId = userId,
                projectCode = projectId,
                imageCode = imageCode,
                imageVersion = imageVersion
            )
        return result.data!!
    }
}
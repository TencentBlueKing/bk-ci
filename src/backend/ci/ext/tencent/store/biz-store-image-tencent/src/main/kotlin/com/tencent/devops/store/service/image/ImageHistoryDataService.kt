package com.tencent.devops.store.service.image

import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_VERSION
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.response.SimpleImageInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ImageHistoryDataService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageDao: ImageDao
) {
    private val logger = LoggerFactory.getLogger(ImageHistoryDataService::class.java)

    /**
     * 根据历史model数据获取其在商店中对应的镜像数据记录
     */
    fun tranferHistoryImage(
        userId: String,
        agentType: ImageAgentTypeEnum,
        value: String?,
        interfaceName: String? = "Anon Interface"
    ): SimpleImageInfo {
        logger.info("$interfaceName:tranferHistoryImage:Input($userId,$agentType,$value)")
        var realImageNameTag = when (agentType) {
            ImageAgentTypeEnum.DOCKER, ImageAgentTypeEnum.IDC -> {
                if (value == DockerVersion.TLINUX1_2.value) {
                    "paas$TLINUX1_2_IMAGE"
                } else if (value == DockerVersion.TLINUX2_2.value) {
                    "paas$TLINUX2_2_IMAGE"
                } else {
                    "paas/bkdevops/$value"
                }
            }
            ImageAgentTypeEnum.PUBLIC_DEVCLOUD -> {
                "devcloud/$value"
            }
        }
        realImageNameTag = realImageNameTag.removePrefix("/")
        // 拆分repoName与tag
        val (repoName, tag) = if (realImageNameTag.contains(":")) {
            val nameAndTag = realImageNameTag.split(":")
            Pair(nameAndTag[0], nameAndTag[1])
        } else {
            Pair(realImageNameTag, "latest")
        }
        val imageRecords = imageDao.listByRepoNameAndTag(
            dslContext = dslContext,
            userId = userId,
            repoName = repoName,
            tag = tag
        )
        if (imageRecords != null && imageRecords.size > 0) {
            logger.info("$interfaceName:tranferHistoryImage:Inner:imageRecords.size={$imageRecords.size}")
            return SimpleImageInfo(
                code = imageRecords[0].get(KEY_IMAGE_CODE) as String,
                name = imageRecords[0].get(KEY_IMAGE_NAME) as String,
                version = imageRecords[0].get(KEY_IMAGE_VERSION) as String
            )
        } else {
            // 不存在这样的镜像
            return SimpleImageInfo(
                code = "",
                name = "",
                version = ""
            )
        }
    }

    companion object {
        private const val TLINUX1_2_IMAGE = "/bkdevops/docker-builder1.2:v1"
        private const val TLINUX2_2_IMAGE = "/bkdevops/docker-builder2.2:v1"
    }
}

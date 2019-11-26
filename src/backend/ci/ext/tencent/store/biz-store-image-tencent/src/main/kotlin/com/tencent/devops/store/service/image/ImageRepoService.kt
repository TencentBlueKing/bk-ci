package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.image.api.ServiceImageResource
import com.tencent.devops.image.pojo.DockerRepo
import com.tencent.devops.store.dao.image.ImageDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLDecoder

@Service
class ImageRepoService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageDao: ImageDao,
    private val client: Client
) {
    private val logger = LoggerFactory.getLogger(ImageRepoService::class.java)

    /**
     * 查找蓝盾仓库关联镜像信息
     */
    fun getBkRelImageInfo(userId: String, imageRepoName: String, imageId: String?): Result<DockerRepo?> {
        logger.info("getRelImageInfo userId is:$userId,imageRepoName is:$imageRepoName,imageId is:$imageId")
        val imageRepo = URLDecoder.decode(imageRepoName, "UTF-8")
        val imageInfoResult = client.get(ServiceImageResource::class)
            .getImageInfo(userId, imageRepo, null, null)
        logger.info("getRelImageInfo imageInfoResult is:$imageInfoResult")
        if (imageInfoResult.isNotOk()) {
            return imageInfoResult
        }
        val dockerRepo = imageInfoResult.data
        val imageRepoUrl = dockerRepo?.repoUrl ?: ""
        if (null != dockerRepo) {
            val imageCode: String?
            if (null != imageId) {
                val imageRecord = imageDao.getImage(dslContext, imageId)
                logger.info("getRelImageInfo imageRecord is:$imageRecord")
                if (imageRecord == null) {
                    return MessageCodeUtil.generateResponseDataObject(
                        CommonMessageCode.PARAMETER_IS_EXIST,
                        arrayOf(imageId),
                        null
                    )
                }
                imageCode = imageRecord.imageCode
                dockerRepo.tags?.forEach {
                    val relFlag = imageDao.countReleaseImageByTag(
                        dslContext = dslContext,
                        imageCode = imageCode,
                        imageRepoUrl = imageRepoUrl,
                        imageRepoName = it.repo!!,
                        imageTag = it.tag
                    ) > 0
                    it.storeFlag = relFlag
                }
                val tags = dockerRepo.tags?.sortedBy {
                    it.storeFlag
                }
                dockerRepo.tags = tags
            }
        }
        logger.info("getRelImageInfo dockerRepo is:$dockerRepo")
        return Result(dockerRepo)
    }
}

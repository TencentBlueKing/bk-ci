package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.constant.LATEST
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
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
     * 查找蓝盾仓库关联镜像信息（过滤掉镜像已被关联过的tag）
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
            var tags = dockerRepo.tags?.filter {
                (imageDao.countByTag(
                    dslContext = dslContext,
                    imageRepoUrl = imageRepoUrl,
                    imageRepoName = it.repo!!,
                    imageTag = it.tag
                ) == 0 && it.tag != LATEST)
            }?.toMutableList()
            if (null != imageId) {
                // 追加需要回显的镜像tag
                val imageRecord = imageDao.getImage(dslContext, imageId)
                if (null != imageRecord && !imageRecord.imageTag.isNullOrBlank() &&
                    imageRepo == imageRecord.imageRepoName && imageRepoUrl == imageRecord.imageRepoUrl) {
                    val tagInfoResult = client.get(ServiceImageResource::class)
                        .getTagInfo(userId, imageRepo, imageRecord.imageTag)
                    logger.info("getRelImageInfo tagInfoResult is:$tagInfoResult")
                    if (tagInfoResult.isNotOk()) {
                        return Result(tagInfoResult.status, tagInfoResult.message, null)
                    }
                    val imageTag = tagInfoResult.data
                    if (null != imageTag) {
                        if (tags == null) tags = mutableListOf()
                        tags.add(imageTag)
                    }
                }
            }
            dockerRepo.tags = tags
        }
        logger.info("getRelImageInfo dockerRepo is:$dockerRepo")
        return Result(dockerRepo)
    }
}

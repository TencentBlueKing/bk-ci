package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.dao.image.MarketImageDao
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.request.ImageBaseInfoUpdateRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MarketImageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageDao: ImageDao,
    private val marketImageDao: MarketImageDao
) {
    private val logger = LoggerFactory.getLogger(MarketImageService::class.java)

    /**
     * 根据镜像ID和镜像代码判断镜像是否存在
     */
    fun judgeImageExistByIdAndCode(imageId: String, imageCode: String): Result<Boolean> {
        logger.info("the imageId is:$imageId, imageCode is:$imageCode")
        val count = marketImageDao.countByIdAndCode(
            dslContext = dslContext,
            imageId = imageId,
            imageCode = imageCode
        )
        if (count < 1) {
            return MessageCodeUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("imageId:$imageId,imageCode:$imageCode"),
                data = false
            )
        }
        return Result(true)
    }

    fun setImageBuildStatusByImageId(
        imageId: String,
        userId: String,
        imageStatus: ImageStatusEnum,
        msg: String?
    ): Result<Boolean> {
        logger.info("the update userId is :$userId,imageCode is :$imageId,imageStatus is :$imageStatus,msg is :$msg")
        val imageRecord = imageDao.getImage(dslContext, imageId)
        logger.info("the imageRecord is :$imageRecord")
        if (null == imageRecord) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(imageId),
                false
            )
        } else {
            if (imageStatus == ImageStatusEnum.TESTING) {
                // 只有处于验证中的镜像才允许改构建结束后的状态
                // 若验证成功，只有处于验证中的镜像才更改状态
                if (ImageStatusEnum.CHECKING.status.toByte() == imageRecord.imageStatus) {
                    marketImageDao.updateImageStatusById(
                        dslContext = dslContext,
                        imageId = imageRecord.id,
                        imageStatus = imageStatus.status.toByte(),
                        userId = userId,
                        msg = msg
                    )
                }
            } else {
                // 验证失败始终置于失败状态
                marketImageDao.updateImageStatusById(
                    dslContext = dslContext,
                    imageId = imageRecord.id,
                    imageStatus = imageStatus.status.toByte(),
                    userId = userId,
                    msg = msg
                )
            }
        }
        return Result(true)
    }

    fun updateImageBaseInfo(
        userId: String,
        projectCode: String,
        imageCode: String,
        version: String,
        imageBaseInfoUpdateRequest: ImageBaseInfoUpdateRequest
    ): Result<Boolean> {
        logger.info("updateImageBaseInfo userId is:$userId, projectCode is:$projectCode, imageCode is:$imageCode")
        logger.info("updateImageBaseInfo version is:$version, imageBaseInfoUpdateRequest is:$imageBaseInfoUpdateRequest")
        val imageRecord = imageDao.getImage(dslContext, imageCode, version.replace("*", ""))
        logger.info("the imageRecord is :$imageRecord")
        return if (null != imageRecord) {
            marketImageDao.updateImageBaseInfo(
                dslContext = dslContext,
                userId = userId,
                imageIdList = listOf(imageRecord.id),
                imageBaseInfoUpdateRequest = imageBaseInfoUpdateRequest
            )
            Result(true)
        } else {
            MessageCodeUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("$imageCode+$version"),
                data = false
            )
        }
    }
}

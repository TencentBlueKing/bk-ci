package com.tencent.devops.store.service.image

import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ImageCommonService @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeProjectRelDao: StoreProjectRelDao
) {
    private val logger = LoggerFactory.getLogger(ImageCommonService::class.java)

    fun generateImageStatusList(
        imageCode: String,
        projectCode: String
    ): MutableList<Byte> {
        val flag = storeProjectRelDao.isInitTestProjectCode(dslContext, imageCode, StoreTypeEnum.IMAGE, projectCode)
        logger.info("the isInitTestProjectCode flag is :$flag")
        // 普通项目的查已发布和下架中的镜像
        var imageStatusList =
            mutableListOf(ImageStatusEnum.RELEASED.status.toByte(), ImageStatusEnum.UNDERCARRIAGING.status.toByte())
        if (flag) {
            // 原生初始化项目有和申请镜像协作者指定的调试项目权查处于测试中、审核中、已发布和下架中的镜像
            imageStatusList = mutableListOf(
                ImageStatusEnum.TESTING.status.toByte(),
                ImageStatusEnum.AUDITING.status.toByte(),
                ImageStatusEnum.RELEASED.status.toByte(),
                ImageStatusEnum.UNDERCARRIAGING.status.toByte()
            )
        }
        return imageStatusList
    }
}

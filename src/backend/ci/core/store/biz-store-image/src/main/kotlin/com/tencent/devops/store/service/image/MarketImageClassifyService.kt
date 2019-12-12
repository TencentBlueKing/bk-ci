package com.tencent.devops.store.service.image

import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.service.common.AbstractClassifyService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("IMAGE_CLASSIFY_SERVICE")
class MarketImageClassifyService : AbstractClassifyService() {

    private val logger = LoggerFactory.getLogger(MarketImageClassifyService::class.java)

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var imageDao: ImageDao

    override fun getDeleteClassifyFlag(classifyId: String): Boolean {
        // 允许删除分类是条件：1、该分类下的镜像都不处于上架状态 2、该分类下的镜像如果处于已下架状态但已经没人在用
        var flag = false
        val releaseImageNum = imageDao.countReleaseImageNumByClassifyId(dslContext, classifyId)
        logger.info("the releaseImageNum is :$releaseImageNum")
        if (releaseImageNum == 0) {
            val undercarriageImageNum = imageDao.countUndercarriageImageNumByClassifyId(dslContext, classifyId)
            logger.info("the undercarriageImageNum is :$undercarriageImageNum")
            if (undercarriageImageNum == 0) {
                flag = true
            }
        }
        return flag
    }
}

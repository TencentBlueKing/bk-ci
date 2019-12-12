package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.dao.image.ImageCategoryRelDao
import com.tencent.devops.store.pojo.common.Category
import com.tencent.devops.store.service.common.StoreCommonService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ImageCategoryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageCategoryRelDao: ImageCategoryRelDao,
    private val storeCommonService: StoreCommonService
) {
    private val logger = LoggerFactory.getLogger(ImageCategoryService::class.java)

    /**
     * 查找镜像范畴
     */
    fun getCategorysByImageId(imageId: String): Result<List<Category>?> {
        logger.info("the imageId is :$imageId")
        val imageCategoryList = mutableListOf<Category>()
        val imageCategoryRecords = imageCategoryRelDao.getCategorysByImageId(dslContext, imageId) // 查询镜像范畴信息
        imageCategoryRecords?.forEach {
            storeCommonService.addCategoryToCategoryList(it, imageCategoryList)
        }
        return Result(imageCategoryList)
    }
}

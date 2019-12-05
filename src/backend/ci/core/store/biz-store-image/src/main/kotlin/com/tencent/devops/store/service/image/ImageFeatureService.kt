package com.tencent.devops.store.service.image

import com.tencent.devops.store.dao.image.ImageFeatureDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ImageFeatureService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageFeatureDao: ImageFeatureDao
) {
    fun isImagePublic(imageCode: String): Boolean {
        val imageFeature = imageFeatureDao.getImageFeature(dslContext, imageCode)
        return imageFeature?.publicFlag ?: false
    }
}

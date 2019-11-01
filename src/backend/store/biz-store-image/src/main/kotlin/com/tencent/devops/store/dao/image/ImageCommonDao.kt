package com.tencent.devops.store.dao.image

import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository(value = "IMAGE_COMMON_DAO")
class ImageCommonDao : AbstractStoreCommonDao() {

    override fun getStoreNameById(dslContext: DSLContext, storeId: String): String? {
        return with(TImage.T_IMAGE) {
            dslContext.select(IMAGE_NAME).from(this).where(ID.eq(storeId)).fetchOne(0, String::class.java)
        }
    }
}
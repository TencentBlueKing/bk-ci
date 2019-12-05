package com.tencent.devops.store.dao.image

import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository(value = "IMAGE_COMMON_DAO")
class ImageCommonDao : AbstractStoreCommonDao() {
    override fun getNewestStoreNameByCode(dslContext: DSLContext, storeCode: String): String? {
        return with(TImage.T_IMAGE) {
            dslContext.select(IMAGE_NAME).from(this)
                .where(IMAGE_CODE.eq(storeCode))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne(0, String::class.java)
        }
    }

    override fun getStoreCodeListByName(dslContext: DSLContext, storeName: String): Result<out Record>? {
        return with(TImage.T_IMAGE) {
            dslContext.select(IMAGE_CODE.`as`("storeCode")).from(this)
                .where(IMAGE_NAME.contains(storeName))
                .groupBy(IMAGE_CODE)
                .fetch()
        }
    }

    override fun getStoreNameById(dslContext: DSLContext, storeId: String): String? {
        return with(TImage.T_IMAGE) {
            dslContext.select(IMAGE_NAME).from(this).where(ID.eq(storeId)).fetchOne(0, String::class.java)
        }
    }
}
package com.tencent.devops.store.dao.image

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TImageLabelRel
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_LABEL_CODE
import com.tencent.devops.store.pojo.common.KEY_LABEL_ID
import com.tencent.devops.store.pojo.common.KEY_LABEL_NAME
import com.tencent.devops.store.pojo.common.KEY_LABEL_TYPE
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Record6
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ImageLabelRelDao {

    fun getImageIdsByLabelIds(
        dslContext: DSLContext,
        labelIds: Set<String>
    ): Result<Record1<String>>? {
        with(TImageLabelRel.T_IMAGE_LABEL_REL) {
            return dslContext.select(IMAGE_ID).from(this)
                .where(LABEL_ID.`in`(labelIds))
                .fetch()
        }
    }

    fun getLabelsByImageId(
        dslContext: DSLContext,
        imageId: String
    ): Result<Record6<String, String, String, Byte, LocalDateTime, LocalDateTime>>? {
        val a = TLabel.T_LABEL.`as`("a")
        val b = TImageLabelRel.T_IMAGE_LABEL_REL.`as`("b")
        return dslContext.select(
            a.ID.`as`(KEY_LABEL_ID),
            a.LABEL_CODE.`as`(KEY_LABEL_CODE),
            a.LABEL_NAME.`as`(KEY_LABEL_NAME),
            a.TYPE.`as`(KEY_LABEL_TYPE),
            a.CREATE_TIME.`as`(KEY_CREATE_TIME),
            a.UPDATE_TIME.`as`(KEY_UPDATE_TIME)
        ).from(a).join(b).on(a.ID.eq(b.LABEL_ID))
            .where(b.IMAGE_ID.eq(imageId))
            .orderBy(a.LABEL_NAME.asc())
            .fetch()
    }

    fun deleteByImageId(dslContext: DSLContext, imageId: String) {
        with(TImageLabelRel.T_IMAGE_LABEL_REL) {
            dslContext.deleteFrom(this)
                .where(IMAGE_ID.eq(imageId))
                .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, userId: String, imageId: String, labelIdList: List<String>?) {
        with(TImageLabelRel.T_IMAGE_LABEL_REL) {
            val addStep = labelIdList?.map {
                dslContext.insertInto(
                    this,
                    ID,
                    IMAGE_ID,
                    LABEL_ID,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        imageId,
                        it,
                        userId,
                        userId
                    )
            }
            dslContext.batch(addStep).execute()
        }
    }
}
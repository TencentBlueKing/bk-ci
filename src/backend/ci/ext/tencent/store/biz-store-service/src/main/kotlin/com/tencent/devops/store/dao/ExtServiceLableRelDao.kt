package com.tencent.devops.store.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TAtomLabelRel
import com.tencent.devops.model.store.tables.TExtensionServiceLabelRel
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_LABEL_CODE
import com.tencent.devops.store.pojo.common.KEY_LABEL_ID
import com.tencent.devops.store.pojo.common.KEY_LABEL_NAME
import com.tencent.devops.store.pojo.common.KEY_LABEL_TYPE
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ExtServiceLableRelDao {

    fun getLabelsByServiceId(
        dslContext: DSLContext,
        serviceId: String
    ): Result<out Record>? {
        val a = TLabel.T_LABEL.`as`("a")
        val b = TExtensionServiceLabelRel.T_EXTENSION_SERVICE_LABEL_REL.`as`("b")
        return dslContext.select(
            a.ID.`as`(KEY_LABEL_ID),
            a.LABEL_CODE.`as`(KEY_LABEL_CODE),
            a.LABEL_NAME.`as`(KEY_LABEL_NAME),
            a.TYPE.`as`(KEY_LABEL_TYPE),
            a.CREATE_TIME.`as`(KEY_CREATE_TIME),
            a.UPDATE_TIME.`as`(KEY_UPDATE_TIME)
        ).from(a).join(b).on(a.ID.eq(b.LABEL_ID))
            .where(b.SERVICE_ID.eq(serviceId))
            .fetch()
    }

    fun deleteByServiceId(dslContext: DSLContext, serviceId: String) {
        with(TExtensionServiceLabelRel.T_EXTENSION_SERVICE_LABEL_REL) {
            dslContext.deleteFrom(this)
                .where(SERVICE_ID.eq(serviceId))
                .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, userId: String, serviceId: String, labelIdList: List<String>) {
        with(TExtensionServiceLabelRel.T_EXTENSION_SERVICE_LABEL_REL) {
            val addStep = labelIdList.map {
                dslContext.insertInto(
                    this,
                    ID,
                    SERVICE_ID,
                    LABEL_ID,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        serviceId,
                        it,
                        userId,
                        userId
                    )
            }
            dslContext.batch(addStep).execute()
        }
    }
}
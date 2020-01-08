package com.tencent.devops.store.dao

import com.tencent.devops.model.store.tables.TExtensionServiceItemRel
import com.tencent.devops.model.store.tables.TExtensionServiceVersionLog
import com.tencent.devops.model.store.tables.records.TExtensionServiceItemRelRecord
import com.tencent.devops.model.store.tables.records.TExtensionServiceVersionLogRecord
import com.tencent.devops.store.pojo.ExtServiceItemRelCreateInfo
import com.tencent.devops.store.pojo.ExtServiceItemRelUpdateInfo
import com.tencent.devops.store.pojo.ExtServiceVersionLogCreateInfo
import com.tencent.devops.store.pojo.ExtServiceVersionLogUpdateInfo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import sun.util.resources.LocaleData
import java.time.LocalDateTime

@Repository
class ExtServiceItemRelDao {
    fun create(
        dslContext: DSLContext,
        userId: String,
        id: String,
        extServiceItemRelCreateInfo: ExtServiceItemRelCreateInfo
    ) {
        with(TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL) {
            dslContext.insertInto(
                this,
                ID,
                SERVICE_ID,
                ITEM_ID,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    id,
                    extServiceItemRelCreateInfo.serviceId,
                    extServiceItemRelCreateInfo.itemId,
                    extServiceItemRelCreateInfo.creatorUser,
                    extServiceItemRelCreateInfo.modifierUser,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    fun updateExtServiceFeatureBaseInfo(
        dslContext: DSLContext,
        userId: String,
        serviceId: String,
        extServiceItemRelUpdateInfo: ExtServiceItemRelUpdateInfo
    ) {
        with(TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL) {
            val baseStep = dslContext.update(this)
            val itemId = extServiceItemRelUpdateInfo.itemId
            if (null != itemId) {
                baseStep.set(ITEM_ID, itemId)
            }

            baseStep.set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(SERVICE_ID.eq(serviceId))
                .execute()
        }
    }

    fun getVersionLogByServiceId(
        dslContext: DSLContext,
        serviceId: String
    ): TExtensionServiceItemRelRecord {
        with(TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL) {
            return dslContext.selectFrom(this).where(SERVICE_ID.eq(serviceId)).fetchOne()
        }
    }
}
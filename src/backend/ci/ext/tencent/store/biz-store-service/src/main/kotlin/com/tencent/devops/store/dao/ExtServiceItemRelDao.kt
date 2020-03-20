package com.tencent.devops.store.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TExtensionServiceItemRel
import com.tencent.devops.model.store.tables.records.TExtensionServiceItemRelRecord
import com.tencent.devops.store.pojo.ExtServiceItemRelCreateInfo
import com.tencent.devops.store.pojo.ExtServiceItemRelUpdateInfo
import com.tencent.devops.store.pojo.ItemPropCreateInfo
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExtServiceItemRelDao {
    fun create(
        dslContext: DSLContext,
        userId: String,
        extServiceItemRelCreateInfo: ExtServiceItemRelCreateInfo
    ) {
        with(TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL) {
            dslContext.insertInto(
                this,
                ID,
                SERVICE_ID,
                ITEM_ID,
                BK_SERVICE_ID,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    UUIDUtil.generate(),
                    extServiceItemRelCreateInfo.serviceId,
                    extServiceItemRelCreateInfo.itemId,
                    extServiceItemRelCreateInfo.bkServiceId,
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

    fun getItemByServiceId(
        dslContext: DSLContext,
        serviceId: String
    ): Result<TExtensionServiceItemRelRecord>? {
        with(TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL) {
            return dslContext.selectFrom(this).where(SERVICE_ID.eq(serviceId)).fetch()
        }
    }

    fun deleteByServiceId(dslContext: DSLContext, serviceId: String) {
        with(TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL) {
            dslContext.deleteFrom(this)
                .where(SERVICE_ID.eq(serviceId))
                .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, userId: String, serviceId: String, itemPropList: List<ItemPropCreateInfo>) {
        with(TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL) {

            val addStep = itemPropList.map {
                dslContext.insertInto(
                    this,
                    ID,
                    SERVICE_ID,
                    ITEM_ID,
                    BK_SERVICE_ID,
                    PROPS,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        serviceId,
                        it.itemId,
                        it.bkServiceId,
                        it.props,
                        userId,
                        userId
                    )
            }
            dslContext.batch(addStep).execute()
        }
    }

    fun batchUpdateServiceItemRel(dslContext: DSLContext, serviceItemRelList: List<TExtensionServiceItemRelRecord>) {
        if (serviceItemRelList.isEmpty()) {
            return
        }
        dslContext.batchUpdate(serviceItemRelList).execute()
    }

    fun getBkService(dslContext: DSLContext): Result<out Record> {
        return with(TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL) {
            dslContext.select(BK_SERVICE_ID.`as`("bkServiceId")).from(this).groupBy(BK_SERVICE_ID).fetch()
        }
    }
}
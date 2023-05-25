package com.tencent.devops.store.dao.common

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStorePublisherMemberRel
import com.tencent.devops.model.store.tables.records.TStorePublisherMemberRelRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PublisherMemberDao {

    fun batchCreatePublisherMemberRel(
        dslContext: DSLContext,
        storePublisherMemberRelInfos: List<TStorePublisherMemberRelRecord>
    ): Int {
        return dslContext.batchInsert(storePublisherMemberRelInfos).execute().size
    }

    fun batchDeletePublisherMemberRelByPublisherId(
        dslContext: DSLContext,
        organizePublishersIds: List<String>
    ) {
        with(TStorePublisherMemberRel.T_STORE_PUBLISHER_MEMBER_REL) {
            dslContext.deleteFrom(this).where(PUBLISHER_ID.`in`(organizePublishersIds)).execute()
        }
    }

    fun batchDeletePublisherMemberByMemberIds(
        dslContext: DSLContext,
        delStorePublisherMemberRelRecords: List<TStorePublisherMemberRelRecord>
    ) {
        with(TStorePublisherMemberRel.T_STORE_PUBLISHER_MEMBER_REL) {
            dslContext.batch(delStorePublisherMemberRelRecords.map {
                dslContext.deleteFrom(this)
                    .where(PUBLISHER_ID.eq(it.publisherId))
                    .and(MEMBER_ID.eq(it.memberId))
            }).execute()
        }
    }

    fun getPublisherMemberRelByMemberId(dslContext: DSLContext, memberId: String): List<String> {
        with(TStorePublisherMemberRel.T_STORE_PUBLISHER_MEMBER_REL) {
            return dslContext.select(PUBLISHER_ID)
                .from(this)
                .where(MEMBER_ID.eq(memberId))
                .fetchInto(String::class.java)
        }
    }

    fun getPublisherMemberRelMemberIdsByPublisherId(dslContext: DSLContext, publisherId: String): List<String> {
        with(TStorePublisherMemberRel.T_STORE_PUBLISHER_MEMBER_REL) {
            return dslContext.select(MEMBER_ID)
                .from(this)
                .where(PUBLISHER_ID.eq(publisherId))
                .fetchInto(String::class.java)
        }
    }

    fun createPublisherMemberRel(
        publisherId: String,
        memberId: String,
        userId: String
    ): TStorePublisherMemberRelRecord {
        val storePublisherMemberRel = TStorePublisherMemberRelRecord()
        storePublisherMemberRel.id = UUIDUtil.generate()
        storePublisherMemberRel.publisherId = publisherId
        storePublisherMemberRel.memberId = memberId
        storePublisherMemberRel.creator = userId
        storePublisherMemberRel.createTime = LocalDateTime.now()
        storePublisherMemberRel.modifier = userId
        storePublisherMemberRel.updateTime = LocalDateTime.now()
        return storePublisherMemberRel
    }
}

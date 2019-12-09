package com.tencent.devops.store.dao.image

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TImageAgentType
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_AGENT_TYPE
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ImageAgentTypeDao {
    fun getAgentTypeByImageCode(dslContext: DSLContext, imageCode: String): Result<Record1<String>>? {
        val tImageAgentType = TImageAgentType.T_IMAGE_AGENT_TYPE.`as`("tImageAgentType")
        return dslContext.select(
            tImageAgentType.AGENT_TYPE.`as`(KEY_IMAGE_AGENT_TYPE)
        ).from(tImageAgentType)
            .where(tImageAgentType.IMAGE_CODE.eq(imageCode))
            .fetch()
    }

    fun deleteAgentTypeByImageCode(dslContext: DSLContext, imageCode: String): Int {
        val tImageAgentType = TImageAgentType.T_IMAGE_AGENT_TYPE.`as`("tImageAgentType")
        return dslContext.deleteFrom(
            tImageAgentType
        )
            .where(tImageAgentType.IMAGE_CODE.eq(imageCode))
            .execute()
    }

    fun addAgentTypeByImageCode(dslContext: DSLContext, imageCode: String, agentType: ImageAgentTypeEnum): Int {
        with(TImageAgentType.T_IMAGE_AGENT_TYPE) {
            val baseQuery = dslContext.insertInto(
                this,
                ID,
                IMAGE_CODE,
                AGENT_TYPE
            )
                .values(
                    UUIDUtil.generate(),
                    imageCode,
                    agentType.name
                )
            return baseQuery.execute()
        }
    }

    fun getImageCodesByAgentType(dslContext: DSLContext, agentType: ImageAgentTypeEnum): Result<Record1<String>>? {
        with(TImageAgentType.T_IMAGE_AGENT_TYPE) {
            val baseQuery = dslContext.select(
                IMAGE_CODE
            ).from(this).where(AGENT_TYPE.eq(agentType.name))
            return baseQuery.fetch()
        }
    }
}
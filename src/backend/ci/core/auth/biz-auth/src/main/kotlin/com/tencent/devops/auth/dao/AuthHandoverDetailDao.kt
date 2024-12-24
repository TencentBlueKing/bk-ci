package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.dto.HandoverDetailDTO
import com.tencent.devops.auth.pojo.enum.HandoverType
import com.tencent.devops.model.auth.tables.TAuthHandoverDetail
import com.tencent.devops.model.auth.tables.records.TAuthHandoverDetailRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.springframework.stereotype.Repository

@Repository
class AuthHandoverDetailDao {
    fun batchCreate(
        dslContext: DSLContext,
        handoverDetailDTOs: List<HandoverDetailDTO>
    ) {
        with(TAuthHandoverDetail.T_AUTH_HANDOVER_DETAIL) {
            handoverDetailDTOs.forEach {
                dslContext.insertInto(
                    this,
                    PROJECT_CODE,
                    FLOW_NO,
                    ITEM_ID,
                    RESOURCE_TYPE,
                    HANDOVER_TYPE
                ).values(
                    it.projectCode,
                    it.flowNo,
                    it.itemId,
                    it.resourceType,
                    it.handoverType.value
                ).execute()
            }
        }
    }

    fun list(
        dslContext: DSLContext,
        projectCode: String,
        flowNos: List<String>,
        resourceType: String?,
        handoverType: HandoverType?
    ): List<HandoverDetailDTO> {
        with(TAuthHandoverDetail.T_AUTH_HANDOVER_DETAIL) {
            return dslContext.selectFrom(this)
                .where(
                    buildQueryConditions(
                        projectCode = projectCode,
                        flowNos = flowNos,
                        resourceType = resourceType,
                        handoverType = handoverType
                    )
                ).fetch().map { it.convert() }
        }
    }

    fun count(
        dslContext: DSLContext,
        projectCode: String,
        flowNos: List<String>,
        resourceType: String?,
        handoverType: HandoverType?
    ): Long {
        with(TAuthHandoverDetail.T_AUTH_HANDOVER_DETAIL) {
            return dslContext.selectCount().from(this)
                .where(
                    buildQueryConditions(
                        projectCode = projectCode,
                        flowNos = flowNos,
                        resourceType = resourceType,
                        handoverType = handoverType
                    )
                ).fetchOne(0, Long::class.java)!!
        }
    }

    fun countWithResourceType(
        dslContext: DSLContext,
        projectCode: String,
        flowNo: String,
        handoverType: HandoverType?
    ): Map<String, Long> {
        with(TAuthHandoverDetail.T_AUTH_HANDOVER_DETAIL) {
            return dslContext.select(RESOURCE_TYPE, count())
                .from(this)
                .where(
                    buildQueryConditions(
                        projectCode = projectCode,
                        flowNos = listOf(flowNo),
                        resourceType = null,
                        handoverType = handoverType
                    )
                ).groupBy(RESOURCE_TYPE)
                .fetch().map { Pair(it.value1(), it.value2().toLong()) }.toMap()
        }
    }

    private fun buildQueryConditions(
        projectCode: String,
        flowNos: List<String>,
        resourceType: String?,
        handoverType: HandoverType?
    ): List<Condition> {
        with(TAuthHandoverDetail.T_AUTH_HANDOVER_DETAIL) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_CODE.eq(projectCode))
            conditions.add(FLOW_NO.`in`(flowNos))
            resourceType?.let {
                conditions.add(RESOURCE_TYPE.eq(resourceType))
            }
            handoverType?.let {
                conditions.add(HANDOVER_TYPE.eq(handoverType.value))
            }
            return conditions
        }
    }

    private fun TAuthHandoverDetailRecord.convert(): HandoverDetailDTO {
        return HandoverDetailDTO(
            projectCode = projectCode,
            flowNo = flowNo,
            itemId = itemId,
            resourceType = resourceType,
            handoverType = HandoverType.get(handoverType)
        )
    }
}

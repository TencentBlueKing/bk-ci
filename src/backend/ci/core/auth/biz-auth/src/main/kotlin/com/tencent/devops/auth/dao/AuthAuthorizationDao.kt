package com.tencent.devops.auth.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationConditionRequest
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationResponse
import com.tencent.devops.model.auth.tables.TAuthResourceAuthorization
import com.tencent.devops.model.auth.tables.records.TAuthResourceAuthorizationRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class AuthAuthorizationDao {
    fun batchAddOrUpdate(
        dslContext: DSLContext,
        resourceAuthorizationList: List<ResourceAuthorizationDTO>
    ) {
        with(TAuthResourceAuthorization.T_AUTH_RESOURCE_AUTHORIZATION) {
            dslContext.batch(
                resourceAuthorizationList.map { resourceAuthorizationDto ->
                    val handoverDateTime = Timestamp(resourceAuthorizationDto.handoverTime!!).toLocalDateTime()
                    dslContext.insertInto(
                        this,
                        PROJECT_CODE,
                        RESOURCE_TYPE,
                        RESOURCE_CODE,
                        RESOURCE_NAME,
                        HANDOVER_FROM,
                        HANDOVER_FROM_CN_NAME,
                        HANDOVER_TIME
                    ).values(
                        resourceAuthorizationDto.projectCode,
                        resourceAuthorizationDto.resourceType,
                        resourceAuthorizationDto.resourceCode,
                        resourceAuthorizationDto.resourceName,
                        resourceAuthorizationDto.handoverFrom,
                        resourceAuthorizationDto.handoverFromCnName,
                        handoverDateTime
                    ).onDuplicateKeyUpdate()
                        .set(HANDOVER_FROM, resourceAuthorizationDto.handoverFrom)
                        .set(HANDOVER_FROM_CN_NAME, resourceAuthorizationDto.handoverFromCnName)
                        .set(RESOURCE_NAME, resourceAuthorizationDto.resourceName)
                        .set(HANDOVER_TIME, handoverDateTime)
                        .set(UPDATE_TIME, LocalDateTime.now())
                }
            ).execute()
        }
    }

    fun batchUpdate(
        dslContext: DSLContext,
        resourceAuthorizationHandoverList: List<ResourceAuthorizationDTO>
    ) {
        with(TAuthResourceAuthorization.T_AUTH_RESOURCE_AUTHORIZATION) {
            dslContext.batch(
                resourceAuthorizationHandoverList.map { resourceAuthorizationDto ->
                    dslContext.update(this)
                        .let {
                            if (resourceAuthorizationDto is ResourceAuthorizationHandoverDTO) {
                                it.set(HANDOVER_FROM, resourceAuthorizationDto.handoverTo)
                                    .set(HANDOVER_FROM_CN_NAME, resourceAuthorizationDto.handoverToCnName)
                                    .set(HANDOVER_TIME, LocalDateTime.now())
                            } else {
                                it
                            }
                        }
                        .set(RESOURCE_NAME, resourceAuthorizationDto.resourceName)
                        .set(UPDATE_TIME, LocalDateTime.now())
                        .where(PROJECT_CODE.eq(resourceAuthorizationDto.projectCode))
                        .and(RESOURCE_TYPE.eq(resourceAuthorizationDto.resourceType))
                        .and(RESOURCE_CODE.eq(resourceAuthorizationDto.resourceCode))
                }
            ).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ) {
        with(TAuthResourceAuthorization.T_AUTH_RESOURCE_AUTHORIZATION) {
            dslContext.deleteFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(RESOURCE_CODE.eq(resourceCode))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): ResourceAuthorizationResponse? {
        return with(TAuthResourceAuthorization.T_AUTH_RESOURCE_AUTHORIZATION) {
            dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(RESOURCE_CODE.eq(resourceCode))
                .fetchAny()?.convert()
        }
    }

    fun list(
        dslContext: DSLContext,
        condition: ResourceAuthorizationConditionRequest
    ): List<ResourceAuthorizationResponse> {
        return with(TAuthResourceAuthorization.T_AUTH_RESOURCE_AUTHORIZATION) {
            dslContext.selectFrom(this)
                .where(buildQueryCondition(condition))
                .let {
                    if (condition.page != null && condition.pageSize != null) {
                        it.limit((condition.page!! - 1) * condition.pageSize!!, condition.pageSize)
                    } else it
                }
                .fetch().map { it.convert() }
        }
    }

    fun count(
        dslContext: DSLContext,
        condition: ResourceAuthorizationConditionRequest
    ): Int {
        return with(TAuthResourceAuthorization.T_AUTH_RESOURCE_AUTHORIZATION) {
            dslContext.selectCount()
                .from(this)
                .where(buildQueryCondition(condition))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun TAuthResourceAuthorization.buildQueryCondition(
        condition: ResourceAuthorizationConditionRequest
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(PROJECT_CODE.eq(condition.projectCode))
        conditions.add(RESOURCE_TYPE.eq(condition.resourceType))
        if (condition.resourceName != null) {
            conditions.add(RESOURCE_NAME.like("%${condition.resourceName}%"))
        }
        if (condition.handoverFrom != null) {
            conditions.add(HANDOVER_FROM.eq(condition.handoverFrom))
        }
        if (condition.greaterThanHandoverTime != null && condition.lessThanHandoverTime != null) {
            conditions.add(HANDOVER_TIME.ge(Timestamp(condition.greaterThanHandoverTime!!).toLocalDateTime()))
            conditions.add(HANDOVER_TIME.le(Timestamp(condition.lessThanHandoverTime!!).toLocalDateTime()))
        }
        return conditions
    }

    fun TAuthResourceAuthorizationRecord.convert(): ResourceAuthorizationResponse {
        return ResourceAuthorizationResponse(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceName = resourceName,
            resourceCode = resourceCode,
            handoverTime = handoverTime.timestampmilli(),
            handoverFrom = handoverFrom,
            handoverFromCnName = handoverFromCnName
        )
    }
}

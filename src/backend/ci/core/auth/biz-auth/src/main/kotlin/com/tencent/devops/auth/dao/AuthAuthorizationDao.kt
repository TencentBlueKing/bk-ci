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
    fun batchAdd(
        dslContext: DSLContext,
        resourceAuthorizationList: List<ResourceAuthorizationDTO>
    ) {
        with(TAuthResourceAuthorization.T_AUTH_RESOURCE_AUTHORIZATION) {
            resourceAuthorizationList.forEach { resourceAuthorizationDto ->
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
                ).execute()
            }
        }
    }

    fun migrate(
        dslContext: DSLContext,
        resourceAuthorizationList: List<ResourceAuthorizationDTO>
    ) {
        with(TAuthResourceAuthorization.T_AUTH_RESOURCE_AUTHORIZATION) {
            resourceAuthorizationList.forEach { resourceAuthorizationDto ->
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
                    .execute()
            }
        }
    }

    @Suppress("NestedBlockDepth")
    fun batchUpdate(
        dslContext: DSLContext,
        resourceAuthorizationHandoverList: List<ResourceAuthorizationDTO>
    ) {
        with(TAuthResourceAuthorization.T_AUTH_RESOURCE_AUTHORIZATION) {
            resourceAuthorizationHandoverList.forEach { resourceAuthorizationDto ->
                dslContext.update(this)
                    .let {
                        if (resourceAuthorizationDto is ResourceAuthorizationHandoverDTO) {
                            it.set(HANDOVER_FROM, resourceAuthorizationDto.handoverTo)
                                .set(HANDOVER_FROM_CN_NAME, resourceAuthorizationDto.handoverToCnName)
                                .set(HANDOVER_TIME, LocalDateTime.now())
                        } else {
                            it.set(HANDOVER_TIME, HANDOVER_TIME)
                        }
                    }
                    .set(RESOURCE_NAME, resourceAuthorizationDto.resourceName)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(PROJECT_CODE.eq(resourceAuthorizationDto.projectCode))
                    .and(RESOURCE_TYPE.eq(resourceAuthorizationDto.resourceType))
                    .and(RESOURCE_CODE.eq(resourceAuthorizationDto.resourceCode))
                    .execute()
            }
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

    fun delete(
        dslContext: DSLContext,
        projectCode: String,
        resourceType: String,
        resourceCodes: List<String>
    ) {
        with(TAuthResourceAuthorization.T_AUTH_RESOURCE_AUTHORIZATION) {
            dslContext.deleteFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(RESOURCE_CODE.notIn(resourceCodes))
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
                .orderBy(HANDOVER_TIME.desc())
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
        conditionReq: ResourceAuthorizationConditionRequest
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        with(conditionReq) {
            conditions.add(PROJECT_CODE.eq(projectCode))
            if (resourceType != null) {
                conditions.add(RESOURCE_TYPE.eq(resourceType))
            }
            if (resourceName != null) {
                conditions.add(RESOURCE_NAME.like("%$resourceName%"))
            }
            if (!filterResourceCodes.isNullOrEmpty()) {
                conditions.add(RESOURCE_CODE.`in`(filterResourceCodes))
            }
            if (!excludeResourceCodes.isNullOrEmpty()) {
                conditions.add(RESOURCE_CODE.notIn(excludeResourceCodes))
            }
            if (handoverFrom != null) {
                conditions.add(HANDOVER_FROM.eq(handoverFrom))
            }
            if (!handoverFroms.isNullOrEmpty()) {
                conditions.add(HANDOVER_FROM.`in`(handoverFroms))
            }
            if (greaterThanHandoverTime != null && lessThanHandoverTime != null) {
                conditions.add(HANDOVER_TIME.ge(Timestamp(greaterThanHandoverTime!!).toLocalDateTime()))
                conditions.add(HANDOVER_TIME.le(Timestamp(lessThanHandoverTime!!).toLocalDateTime()))
            }
        }
        return conditions
    }

    fun listUserProjects(
        dslContext: DSLContext,
        userId: String
    ): List<String> {
        return with(TAuthResourceAuthorization.T_AUTH_RESOURCE_AUTHORIZATION) {
            dslContext.select(PROJECT_CODE)
                .from(this)
                .where(HANDOVER_FROM.eq(userId))
                .groupBy(PROJECT_CODE)
                .fetch().map { it.value1() }
        }
    }

    fun TAuthResourceAuthorizationRecord.convert(): ResourceAuthorizationResponse {
        return ResourceAuthorizationResponse(
            id = id,
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

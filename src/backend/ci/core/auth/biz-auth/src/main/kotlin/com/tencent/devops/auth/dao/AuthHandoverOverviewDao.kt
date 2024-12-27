package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.dto.HandoverOverviewCreateDTO
import com.tencent.devops.auth.pojo.enum.CollationType
import com.tencent.devops.auth.pojo.enum.HandoverStatus
import com.tencent.devops.auth.pojo.enum.SortType
import com.tencent.devops.auth.pojo.request.HandoverOverviewQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewUpdateReq
import com.tencent.devops.auth.pojo.vo.HandoverOverviewVo
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.auth.tables.TAuthHandoverOverview
import com.tencent.devops.model.auth.tables.records.TAuthHandoverOverviewRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthHandoverOverviewDao {
    fun create(
        dslContext: DSLContext,
        overviewDTO: HandoverOverviewCreateDTO
    ) {
        with(TAuthHandoverOverview.T_AUTH_HANDOVER_OVERVIEW) {
            dslContext.insertInto(
                this,
                PROJECT_CODE,
                PROJECT_NAME,
                TITLE,
                FLOW_NO,
                APPLICANT,
                APPROVER,
                STATUS,
                GROUP_COUNT,
                AUTHORIZATION_COUNT,
                REMARK
            ).values(
                overviewDTO.projectCode,
                overviewDTO.projectName,
                overviewDTO.title,
                overviewDTO.flowNo,
                overviewDTO.applicant,
                overviewDTO.approver,
                overviewDTO.handoverStatus.value,
                overviewDTO.groupCount,
                overviewDTO.authorizationCount,
                overviewDTO.remark
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        overviewDTO: HandoverOverviewUpdateReq
    ) {
        with(TAuthHandoverOverview.T_AUTH_HANDOVER_OVERVIEW) {
            dslContext.update(this)
                .set(STATUS, overviewDTO.handoverAction.value)
                .let { if (overviewDTO.remark != null) it.set(REMARK, overviewDTO.remark) else it }
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(LAST_OPERATOR, overviewDTO.operator)
                .where(FLOW_NO.eq(overviewDTO.flowNo))
                .and(PROJECT_CODE.eq(overviewDTO.projectCode))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        flowNo: String
    ): HandoverOverviewVo? {
        return with(TAuthHandoverOverview.T_AUTH_HANDOVER_OVERVIEW) {
            dslContext.selectFrom(this)
                .where(FLOW_NO.eq(flowNo))
                .fetchAny()?.convert()
        }
    }

    fun list(
        dslContext: DSLContext,
        queryRequest: HandoverOverviewQueryReq
    ): List<HandoverOverviewVo> {
        return with(TAuthHandoverOverview.T_AUTH_HANDOVER_OVERVIEW) {
            dslContext.selectFrom(this)
                .where(buildQueryConditions(queryRequest))
                .let {
                    when {
                        queryRequest.sortType == SortType.FLOW_NO &&
                            queryRequest.collationType == CollationType.ASC -> {
                            it.orderBy(FLOW_NO.asc())
                        }

                        queryRequest.sortType == SortType.FLOW_NO &&
                            queryRequest.collationType == CollationType.DESC -> {
                            it.orderBy(FLOW_NO.desc())
                        }

                        queryRequest.sortType == SortType.CREATE_TIME &&
                            queryRequest.collationType == CollationType.ASC -> {
                            it.orderBy(CREATE_TIME.asc())
                        }

                        queryRequest.sortType == SortType.CREATE_TIME &&
                            queryRequest.collationType == CollationType.DESC -> {
                            it.orderBy(CREATE_TIME.desc())
                        }

                        else -> {
                            it.orderBy(FLOW_NO.desc())
                        }
                    }
                }
                .let {
                    if (queryRequest.page != null && queryRequest.pageSize != null) {
                        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(queryRequest.page, queryRequest.pageSize)
                        it.limit(sqlLimit.limit).offset(sqlLimit.offset)
                    } else {
                        it
                    }
                }
                .skipCheck()
                .fetch()
                .map { it.convert(queryRequest.memberId) }
        }
    }

    fun count(
        dslContext: DSLContext,
        queryRequest: HandoverOverviewQueryReq
    ): Long {
        return with(TAuthHandoverOverview.T_AUTH_HANDOVER_OVERVIEW) {
            dslContext.selectCount().from(this)
                .where(buildQueryConditions(queryRequest))
                .fetchOne(0, Long::class.java)!!
        }
    }

    private fun buildQueryConditions(
        queryRequest: HandoverOverviewQueryReq
    ): List<Condition> {
        with(TAuthHandoverOverview.T_AUTH_HANDOVER_OVERVIEW) {
            val conditions = mutableListOf<Condition>()
            conditions.add(APPROVER.eq(queryRequest.memberId).or(APPLICANT.eq(queryRequest.memberId)))
            queryRequest.projectCode?.let { conditions.add(PROJECT_CODE.eq(queryRequest.projectCode)) }
            queryRequest.projectName?.let { conditions.add(PROJECT_NAME.like("%${queryRequest.projectName}%")) }
            queryRequest.title?.let { conditions.add(TITLE.like("%${queryRequest.title}%")) }
            queryRequest.flowNo?.let { conditions.add(FLOW_NO.eq(queryRequest.flowNo)) }
            queryRequest.flowNos?.let { conditions.add(FLOW_NO.`in`(queryRequest.flowNos)) }
            queryRequest.applicant?.let { conditions.add(APPLICANT.like("%${queryRequest.applicant}%")) }
            queryRequest.approver?.let { conditions.add(APPROVER.like("%${queryRequest.approver}%")) }
            queryRequest.handoverStatus?.let { conditions.add(STATUS.eq(queryRequest.handoverStatus!!.value)) }
            queryRequest.minCreatedTime?.let {
                conditions.add(
                    CREATE_TIME.ge(
                        DateTimeUtil.convertTimestampToLocalDateTime(it / 1000)
                    )
                )
            }
            queryRequest.maxCreatedTime?.let {
                conditions.add(
                    CREATE_TIME.le(
                        DateTimeUtil.convertTimestampToLocalDateTime(it / 1000)
                    )
                )
            }
            return conditions
        }
    }

    fun TAuthHandoverOverviewRecord.convert(memberId: String? = null): HandoverOverviewVo {
        return HandoverOverviewVo(
            id = id,
            projectCode = projectCode,
            projectName = projectName,
            title = title,
            flowNo = flowNo,
            applicant = applicant,
            approver = approver,
            handoverStatus = HandoverStatus.get(status),
            groupCount = groupCount,
            authorizationCount = authorizationCount,
            lastOperator = lastOperator,
            createTime = createTime,
            canRevoke = memberId?.let { memberId == applicant && status == HandoverStatus.PENDING.value },
            canApproval = memberId?.let { memberId == approver && status == HandoverStatus.PENDING.value },
            remark = remark
        )
    }
}

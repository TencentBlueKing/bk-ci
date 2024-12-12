package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.constant.AuthI18nConstants
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthAuthorizationDao
import com.tencent.devops.auth.dao.AuthHandoverDetailDao
import com.tencent.devops.auth.dao.AuthHandoverOverviewDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.dto.HandoverDetailDTO
import com.tencent.devops.auth.pojo.dto.HandoverOverviewCreateDTO
import com.tencent.devops.auth.pojo.enum.HandoverStatus
import com.tencent.devops.auth.pojo.enum.HandoverType
import com.tencent.devops.auth.pojo.request.HandoverDetailsQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewUpdateReq
import com.tencent.devops.auth.pojo.vo.HandoverAuthorizationDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverGroupDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverOverviewVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.service.iam.PermissionHandoverApplicationService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationConditionRequest
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class RbacPermissionHandoverApplicationService(
    private val dslContext: DSLContext,
    private val handoverOverviewDao: AuthHandoverOverviewDao,
    private val handoverDetailDao: AuthHandoverDetailDao,
    private val authorizationDao: AuthAuthorizationDao,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val rbacCacheService: RbacCacheService,
    private val redisOperation: RedisOperation
) : PermissionHandoverApplicationService {
    override fun createHandoverApplication(
        overview: HandoverOverviewCreateDTO,
        details: List<HandoverDetailDTO>
    ) {
        logger.info("create handover application:{}|{}", overview, details)
        // todo 发送邮件/devops-notices通知

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            handoverOverviewDao.create(
                dslContext = transactionContext,
                overviewDTO = overview
            )
            handoverDetailDao.batchCreate(
                dslContext = transactionContext,
                handoverDetailDTOs = details
            )
        }
    }

    override fun generateTitle(
        groupCount: Int,
        authorizationCount: Int
    ): String {
        return I18nUtil.getCodeLanMessage(messageCode = AuthI18nConstants.BK_APPLY_TO_HANDOVER).let {
            when {
                groupCount > 0 && authorizationCount > 0 -> {
                    it.plus(I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_HANDOVER_GROUPS, params = arrayOf(groupCount.toString()))).plus(",").plus(
                        I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_HANDOVER_AUTHORIZATIONS, params = arrayOf(authorizationCount.toString()))
                    )
                }

                groupCount > 0 -> {
                    it.plus(I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_HANDOVER_GROUPS, params = arrayOf(groupCount.toString())))
                }

                else -> {
                    it.plus(
                        I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_HANDOVER_AUTHORIZATIONS, params = arrayOf(authorizationCount.toString()))
                    )
                }
            }
        }
    }

    /**
     * 生成格式如 REQ2024111300001
     * REQ 固定前缀
     * 20241113 表示日期
     * 00001 表示当天第几个单号
     * */
    override fun generateFlowNo(): String {
        val currentTime = DateTimeUtil.toDateTime(LocalDateTime.now(), DateTimeUtil.YYYYMMDD)
        val incrementedValue = redisOperation.increment(String.format(FLOW_NO_KEY, currentTime), 1)
        val formattedIncrementedValue = String.format("%05d", incrementedValue)
        return FLOW_NO_PREFIX + currentTime + formattedIncrementedValue
    }

    override fun updateHandoverApplication(overview: HandoverOverviewUpdateReq) {
        logger.info("update handover application:{}", overview)
        handoverOverviewDao.update(
            dslContext = dslContext,
            overviewDTO = overview
        )
    }

    override fun getHandoverOverview(flowNo: String): HandoverOverviewVo {
        return handoverOverviewDao.get(
            dslContext = dslContext,
            flowNo = flowNo
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_HANDOVER_OVERVIEW_NOT_EXIST
        )
    }

    override fun listHandoverOverviews(
        queryRequest: HandoverOverviewQueryReq
    ): SQLPage<HandoverOverviewVo> {
        logger.info("list handover overviews :$queryRequest")
        val records = handoverOverviewDao.list(
            dslContext = dslContext,
            queryRequest = queryRequest
        )
        val count = handoverOverviewDao.count(
            dslContext = dslContext,
            queryRequest = queryRequest
        )
        return SQLPage(
            records = records,
            count = count
        )
    }

    override fun listAuthorizationsOfHandoverApplication(
        queryReq: HandoverDetailsQueryReq
    ): SQLPage<HandoverAuthorizationDetailVo> {
        logger.info("list authorizations of handover application :$queryReq")
        val flowNo = queryReq.flowNo!!
        val overview = getHandoverOverview(flowNo)
        val resourceCodes = handoverDetailDao.list(
            dslContext = dslContext,
            projectCode = overview.projectCode,
            flowNos = listOf(flowNo),
            resourceType = queryReq.resourceType,
            handoverType = HandoverType.AUTHORIZATION
        ).map { it.itemId }
        val count = handoverDetailDao.count(
            dslContext = dslContext,
            projectCode = overview.projectCode,
            flowNos = listOf(flowNo),
            resourceType = queryReq.resourceType,
            handoverType = HandoverType.AUTHORIZATION
        )
        val records = authorizationDao.list(
            dslContext = dslContext,
            condition = ResourceAuthorizationConditionRequest(
                projectCode = overview.projectCode,
                resourceType = queryReq.resourceType,
                filterResourceCodes = resourceCodes,
                page = queryReq.page,
                pageSize = queryReq.pageSize
            )
        ).map {
            HandoverAuthorizationDetailVo(
                resourceCode = it.resourceCode,
                resourceName = it.resourceName,
                handoverType = HandoverType.AUTHORIZATION,
                handoverFrom = overview.applicant
            )
        }
        return SQLPage(records = records, count = count)
    }

    override fun listGroupsOfHandoverApplication(
        queryReq: HandoverDetailsQueryReq
    ): SQLPage<HandoverGroupDetailVo> {
        logger.info("list groups of handover application :$queryReq")
        val flowNo = queryReq.flowNo!!
        val handoverOverview = getHandoverOverview(flowNo)
        val iamGroupIdsByHandover = listHandoverDetails(
            projectCode = handoverOverview.projectCode,
            flowNo = flowNo,
            resourceType = queryReq.resourceType,
            handoverType = HandoverType.GROUP
        ).map { it.itemId }
        if (iamGroupIdsByHandover.isEmpty())
            return SQLPage(0, emptyList())
        val convertPageSizeToSQLLimit = PageUtil.convertPageSizeToSQLLimit(
            page = queryReq.page,
            pageSize = queryReq.pageSize
        )
        val records = authResourceGroupDao.listGroupByResourceType(
            dslContext = dslContext,
            projectCode = handoverOverview.projectCode,
            resourceType = queryReq.resourceType,
            iamGroupIds = iamGroupIdsByHandover,
            offset = convertPageSizeToSQLLimit.offset,
            limit = convertPageSizeToSQLLimit.limit
        ).map {
            HandoverGroupDetailVo(
                projectCode = it.projectCode,
                iamGroupId = it.relationId,
                groupName = it.groupName,
                groupDesc = it.description,
                resourceCode = it.resourceCode,
                resourceName = it.resourceName
            )
        }
        return SQLPage(
            count = iamGroupIdsByHandover.size.toLong(),
            records = records
        )
    }

    override fun getResourceType2CountOfHandoverApplication(flowNo: String): List<ResourceType2CountVo> {
        logger.info("get resource type count of handover application:$flowNo")
        val handoverOverview = getHandoverOverview(flowNo)
        val resourceType2CountWithGroup = handoverDetailDao.countWithResourceType(
            dslContext = dslContext,
            projectCode = handoverOverview.projectCode,
            flowNo = flowNo,
            handoverType = HandoverType.GROUP
        )
        val resourceType2CountWithAuthorization = handoverDetailDao.countWithResourceType(
            dslContext = dslContext,
            projectCode = handoverOverview.projectCode,
            flowNo = flowNo,
            handoverType = HandoverType.AUTHORIZATION
        )
        val result = mutableListOf<ResourceType2CountVo>()
        if (resourceType2CountWithGroup.isNotEmpty()) {
            result.addAll(
                rbacCacheService.convertResourceType2Count(
                    resourceType2Count = resourceType2CountWithGroup,
                    type = HandoverType.GROUP
                )
            )
        }
        if (resourceType2CountWithAuthorization.isNotEmpty()) {
            result.addAll(
                rbacCacheService.convertResourceType2Count(
                    resourceType2Count = resourceType2CountWithAuthorization,
                    type = HandoverType.AUTHORIZATION
                )
            )
        }
        return result
    }

    override fun listHandoverDetails(
        projectCode: String,
        flowNo: String,
        resourceType: String?,
        handoverType: HandoverType?
    ): List<HandoverDetailDTO> {
        return handoverDetailDao.list(
            dslContext = dslContext,
            projectCode = projectCode,
            flowNos = listOf(flowNo),
            resourceType = resourceType,
            handoverType = handoverType
        )
    }

    override fun listMemberHandoverDetails(
        projectCode: String,
        memberId: String,
        handoverType: HandoverType,
        resourceType: String?
    ): List<HandoverDetailDTO> {
        logger.info("list member handover details:$projectCode|$memberId|$handoverType|$resourceType")
        val handoverOverviews = listHandoverOverviews(
            queryRequest = HandoverOverviewQueryReq(
                memberId = memberId,
                projectCode = projectCode,
                applicant = memberId,
                handoverStatus = HandoverStatus.PENDING
            )
        ).records
        val flowNos = handoverOverviews.map { it.flowNo }
        val flowNo2Approver = handoverOverviews.associate { Pair(it.flowNo, it.approver) }
        return handoverDetailDao.list(
            dslContext = dslContext,
            projectCode = projectCode,
            flowNos = flowNos,
            resourceType = resourceType,
            handoverType = handoverType
        ).map { it.copy(approver = flowNo2Approver[it.flowNo]) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionHandoverApplicationService::class.java)
        private const val FLOW_NO_PREFIX = "REQ"
        private const val FLOW_NO_KEY = "AUTH:HANDOVER:FLOW:NO:%s"
    }
}

package com.tencent.devops.auth.provider.rbac.service

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_APPLY_TO_HANDOVER
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_HANDOVER_AUTHORIZATIONS
import com.tencent.devops.auth.constant.AuthI18nConstants.BK_HANDOVER_GROUPS
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
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionHandoverApplicationService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationConditionRequest
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Suppress("ALL")
class RbacPermissionHandoverApplicationService(
    private val dslContext: DSLContext,
    private val handoverOverviewDao: AuthHandoverOverviewDao,
    private val handoverDetailDao: AuthHandoverDetailDao,
    private val authorizationDao: AuthAuthorizationDao,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val rbacCommonService: RbacCommonService,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val config: CommonConfig,
    private val deptService: DeptService
) : PermissionHandoverApplicationService {
    override fun createHandoverApplication(
        overview: HandoverOverviewCreateDTO,
        details: List<HandoverDetailDTO>
    ): String {
        logger.info("create handover application:{}|{}", overview, details)
        val flowNo = generateFlowNo()
        val (title, handoverOverviewContentOfEmail, handoverOverviewContentOfRtx) =
            generateOverviewContent(
                groupCount = overview.groupCount,
                authorizationCount = overview.authorizationCount
            )
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            handoverOverviewDao.create(
                dslContext = transactionContext,
                overviewDTO = overview.copy(
                    flowNo = flowNo,
                    title = title
                )
            )
            handoverDetailDao.batchCreate(
                dslContext = transactionContext,
                handoverDetailDTOs = details.map { it.copy(flowNo = flowNo) }
            )
        }
        val handoverFromCnName = deptService.getMemberInfo(overview.applicant, ManagerScopesEnum.USER).displayName
        val handoverToCnName = deptService.getMemberInfo(overview.approver, ManagerScopesEnum.USER).displayName
        val resourceType2CountOfHandover = getResourceType2CountOfHandoverApplication(flowNo)
        val handoverOverviewTableBuilder = StringBuilder()
        resourceType2CountOfHandover.forEach {
            handoverOverviewTableBuilder.append(
                String.format(
                    HANDOVER_APPLICATION_TABLE_OF_EMAIL, it.type.alias, it.resourceTypeName, it.count
                )
            )
        }
        val handoverOverviewTable = handoverOverviewTableBuilder.toString()
        val bodyParams = mapOf(
            "handoverFrom" to overview.applicant.plus("（$handoverFromCnName）"),
            "handoverTo" to overview.approver.plus("（$handoverToCnName）"),
            "projectName" to overview.projectName,
            "handoverOverviews" to handoverOverviewContentOfEmail,
            "handoverOverviewContentOfRtx" to handoverOverviewContentOfRtx,
            "table" to handoverOverviewTable,
            "url" to String.format(handoverApplicationUrl, flowNo)
        )
        logger.info("send handover application email:{} ", bodyParams)
        val request = SendNotifyMessageTemplateRequest(
            templateCode = HANDOVER_APPLICATION_TEMPLATE_CODE,
            bodyParams = bodyParams,
            titleParams = bodyParams,
            notifyType = mutableSetOf(NotifyType.RTX.name, NotifyType.EMAIL.name),
            receivers = mutableSetOf(overview.approver)
        )
        kotlin.runCatching {
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        }.onFailure {
            logger.warn("notify email fail ${it.message}|$bodyParams|${overview.approver}")
        }
        return flowNo
    }

    private fun generateOverviewContent(
        groupCount: Int,
        authorizationCount: Int
    ): Triple<String, String, String> {
        val bkHandoverGroups = I18nUtil.getCodeLanMessage(BK_HANDOVER_GROUPS)
        val bkHandoverAuthorizations = I18nUtil.getCodeLanMessage(BK_HANDOVER_AUTHORIZATIONS)
        var titleOfApplication = I18nUtil.getCodeLanMessage(BK_APPLY_TO_HANDOVER)
        var handoverOverviewContentOfEmail = ""
        var handoverOverviewContentOfRtx = ""
        when {
            groupCount > 0 && authorizationCount > 0 -> {
                titleOfApplication = titleOfApplication.plus(" $groupCount ").plus(
                    bkHandoverGroups.plus("，").plus(" $authorizationCount ").plus(bkHandoverAuthorizations)
                )
                handoverOverviewContentOfEmail = """<span class="num">$groupCount</span>$bkHandoverGroups，<span class="num">$authorizationCount</span>$bkHandoverAuthorizations""".trimMargin()
                handoverOverviewContentOfRtx = handoverOverviewContentOfRtx.plus(groupCount).plus(
                    bkHandoverGroups.plus("，").plus(authorizationCount).plus(bkHandoverAuthorizations)
                )
            }

            groupCount > 0 -> {
                titleOfApplication = titleOfApplication.plus(" $groupCount ").plus(bkHandoverGroups)
                handoverOverviewContentOfEmail = """<span class="num">$groupCount</span>$bkHandoverGroups""".trimMargin()
                handoverOverviewContentOfRtx = handoverOverviewContentOfRtx.plus(groupCount).plus(bkHandoverGroups)
            }

            else -> {
                titleOfApplication = titleOfApplication.plus(" $authorizationCount ").plus(bkHandoverAuthorizations)
                handoverOverviewContentOfEmail = """<span class="num">$authorizationCount</span>$bkHandoverAuthorizations""".trimMargin()
                handoverOverviewContentOfRtx = handoverOverviewContentOfRtx.plus(authorizationCount).plus(bkHandoverAuthorizations)
            }
        }
        return Triple(titleOfApplication, handoverOverviewContentOfEmail, handoverOverviewContentOfRtx)
    }

    /**
     * 生成格式如 REQ2024111300001
     * REQ 固定前缀
     * 20241113 表示日期
     * 00001 表示当天第几个单号
     * */
    override fun generateFlowNo(): String {
        val currentTime = DateTimeUtil.toDateTime(LocalDateTime.now(), DateTimeUtil.YYYYMMDD)
        val key = String.format(FLOW_NO_KEY, currentTime)
        val incrementedValue = redisOperation.increment(key, 1)
        redisOperation.expire(key, 3600 * 24)
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
                rbacCommonService.convertResourceType2Count(
                    resourceType2Count = resourceType2CountWithGroup,
                    type = HandoverType.GROUP
                )
            )
        }
        if (resourceType2CountWithAuthorization.isNotEmpty()) {
            result.addAll(
                rbacCommonService.convertResourceType2Count(
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

    private val handoverApplicationUrl = "${config.devopsHostGateway}/console/permission/my-handover?" +
        "type=handoverToMe&flowNo=%s"

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionHandoverApplicationService::class.java)
        private const val FLOW_NO_PREFIX = "REQ"
        private const val FLOW_NO_KEY = "AUTH:HANDOVER:FLOW:NO:%s"
        private const val HANDOVER_APPLICATION_TABLE_OF_EMAIL = "<tr><td style=\"font-size: 14px;\"  align=\"center\">%s</td><td style=\"font-size: 14px;\"  align=\"center\">%s</td><td style=\"font-size: 14px;\"  align=\"center\">%s</td></tr>"
        private const val HANDOVER_APPLICATION_TEMPLATE_CODE = "BK_PERMISSIONS_HANDOVER_APPLICATION"
    }
}

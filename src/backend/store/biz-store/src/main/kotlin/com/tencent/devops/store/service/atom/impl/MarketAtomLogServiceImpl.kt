package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.MarketAtomLogService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 插件市场-插件日志业务逻辑类
 * since: 2019-08-15
 */
@Service
class MarketAtomLogServiceImpl @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val atomPipelineRelDao: StorePipelineRelDao,
    private val storeMemberDao: StoreMemberDao
) : MarketAtomLogService {
    private val logger = LoggerFactory.getLogger(MarketAtomLogServiceImpl::class.java)

    override fun getInitLogs(
        userId: String,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs?> {
        logger.info("getInitLogs userId is:$userId,projectCode is:$projectCode,pipelineId is:$pipelineId,buildId is:$buildId")
        logger.info("getInitLogs isAnalysis is:$isAnalysis,queryKeywords is:$queryKeywords,tag is:$tag,executeCount is:$executeCount")
        val validateResult = validateUserQueryPermission(pipelineId, userId)
        logger.info("getInitLogs validateResult is:$validateResult")
        if (validateResult.isNotOk()) {
            return Result(status = validateResult.status, message = validateResult.message, data = null)
        }
        val queryLogsResult = client.get(ServiceLogResource::class)
            .getInitLogs(
                projectId = projectCode,
                pipelineId = pipelineId,
                buildId = buildId,
                isAnalysis = isAnalysis,
                queryKeywords = queryKeywords,
                tag = tag,
                executeCount = executeCount
            )
        if (queryLogsResult.isNotOk()) {
            return Result(status = queryLogsResult.status, message = queryLogsResult.message, data = null)
        }
        return queryLogsResult
    }

    override fun getAfterLogs(
        userId: String,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        isAnalysis: Boolean?,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs?> {
        logger.info("getAfterLogs userId is:$userId,projectCode is:$projectCode,pipelineId is:$pipelineId,buildId is:$buildId,start is:$start")
        logger.info("getAfterLogs isAnalysis is:$isAnalysis,queryKeywords is:$queryKeywords,tag is:$tag,executeCount is:$executeCount")
        val validateResult = validateUserQueryPermission(pipelineId, userId)
        logger.info("getAfterLogs validateResult is:$validateResult")
        if (validateResult.isNotOk()) {
            return Result(status = validateResult.status, message = validateResult.message, data = null)
        }
        val queryLogsResult = client.get(ServiceLogResource::class)
            .getAfterLogs(
                projectId = projectCode,
                pipelineId = pipelineId,
                buildId = buildId,
                start = start,
                isAnalysis = isAnalysis,
                queryKeywords = queryKeywords,
                tag = tag,
                executeCount = executeCount
            )
        if (queryLogsResult.isNotOk()) {
            return Result(status = queryLogsResult.status, message = queryLogsResult.message, data = null)
        }
        return queryLogsResult
    }

    override fun getMoreLogs(
        userId: String,
        projectCode: String,
        pipelineId: String,
        buildId: String,
        num: Int?,
        fromStart: Boolean?,
        start: Long,
        end: Long,
        tag: String?,
        executeCount: Int?
    ): Result<QueryLogs?> {
        logger.info("getMoreLogs userId is:$userId,projectCode is:$projectCode,pipelineId is:$pipelineId,buildId is:$buildId,num is:$num")
        logger.info("getMoreLogs fromStart is:$fromStart,start is:$start,end is:$end,tag is:$tag,executeCount is:$executeCount")
        val validateResult = validateUserQueryPermission(pipelineId, userId)
        logger.info("getMoreLogs validateResult is:$validateResult")
        if (validateResult.isNotOk()) {
            return Result(status = validateResult.status, message = validateResult.message, data = null)
        }
        val queryLogsResult = client.get(ServiceLogResource::class)
            .getMoreLogs(
                projectId = projectCode,
                pipelineId = pipelineId,
                buildId = buildId,
                num = num,
                fromStart = fromStart,
                start = start,
                end = end,
                tag = tag,
                executeCount = executeCount
            )
        if (queryLogsResult.isNotOk()) {
            return Result(status = queryLogsResult.status, message = queryLogsResult.message, data = null)
        }
        return queryLogsResult
    }

    private fun validateUserQueryPermission(pipelineId: String, userId: String): Result<Boolean> {
        // 查询是否是插件的成员，只有插件的成员才能看日志
        val atomPipelineRelRecord = atomPipelineRelDao.getStorePipelineRelByPipelineId(dslContext, pipelineId)
        logger.info("validateUserQueryPermission atomPipelineRelRecord is:$atomPipelineRelRecord")
        if (null == atomPipelineRelRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(pipelineId))
        }
        val flag = storeMemberDao.isStoreMember(dslContext, userId, atomPipelineRelRecord.storeCode, StoreTypeEnum.ATOM.type.toByte())
        if (!flag) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        return Result(true)
    }
}

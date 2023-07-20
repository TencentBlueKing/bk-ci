/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.HIDDEN_SYMBOL
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.constant.LATEST_EXECUTE_TIME
import com.tencent.devops.common.api.constant.LATEST_EXECUTOR
import com.tencent.devops.common.api.constant.LATEST_MODIFIER
import com.tencent.devops.common.api.constant.LATEST_UPDATE_TIME
import com.tencent.devops.common.api.constant.PIPELINE_URL
import com.tencent.devops.common.api.constant.VERSION
import com.tencent.devops.common.api.enums.TaskStatusEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.CsvUtil
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.GET_PIPELINE_ATOM_INFO_NO_PERMISSION
import com.tencent.devops.process.dao.PipelineAtomReplaceBaseDao
import com.tencent.devops.process.dao.PipelineAtomReplaceItemDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineAtomRel
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.KEY_PROJECT_ID
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.api.common.ServiceStoreResource
import com.tencent.devops.store.pojo.atom.AtomProp
import com.tencent.devops.store.pojo.atom.AtomReplaceRequest
import com.tencent.devops.store.pojo.atom.AtomReplaceRollBack
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.text.MessageFormat
import java.time.LocalDateTime
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Response
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

@Service
@RefreshScope
@Suppress("NestedBlockDepth", "LongMethod", "LongParameterList", "MagicNumber")
class PipelineAtomService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineModelTaskDao: PipelineModelTaskDao,
    private val pipelineAtomReplaceBaseDao: PipelineAtomReplaceBaseDao,
    private val pipelineAtomReplaceItemDao: PipelineAtomReplaceItemDao,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineAtomService::class.java)
        private const val DEFAULT_PAGE_SIZE = 50
    }

    @Value("\${pipeline.editPath:}")
    private val pipelineEditPath: String = ""

    @Value("\${pipeline.atom.maxRelQueryNum:2000}")
    private val maxRelQueryNum: Int = 2000

    @Value("\${pipeline.atom.maxRelQueryRangeTime:90}")
    private val maxRelQueryRangeTime: Long = 90

    fun createReplaceAtomInfo(
        userId: String,
        projectId: String?,
        atomReplaceRequest: AtomReplaceRequest
    ): Result<String> {
        logger.info("createReplaceAtomInfo [$userId|$projectId|$atomReplaceRequest]")
        val baseId = UUIDUtil.generate()
        val fromAtomCode = atomReplaceRequest.fromAtomCode
        val toAtomCode = atomReplaceRequest.toAtomCode
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            pipelineAtomReplaceBaseDao.createAtomReplaceBase(
                dslContext = context,
                baseId = baseId,
                projectId = projectId,
                pipelineIdList = atomReplaceRequest.pipelineIdList,
                fromAtomCode = fromAtomCode,
                toAtomCode = toAtomCode,
                userId = userId
            )
            pipelineAtomReplaceItemDao.createAtomReplaceItem(
                dslContext = context,
                baseId = baseId,
                fromAtomCode = fromAtomCode,
                toAtomCode = toAtomCode,
                versionInfoList = atomReplaceRequest.versionInfoList,
                userId = userId
            )
        }
        return Result(baseId)
    }

    fun atomReplaceRollBack(
        userId: String,
        atomReplaceRollBack: AtomReplaceRollBack
    ): Result<Boolean> {
        logger.info("atomReplaceRollBack [$userId|$atomReplaceRollBack]")
        val baseId = atomReplaceRollBack.baseId
        val itemId = atomReplaceRollBack.itemId
        // 将任务状态更新为”待回滚“状态
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            pipelineAtomReplaceBaseDao.updateAtomReplaceBase(
                dslContext = context,
                baseId = baseId,
                status = TaskStatusEnum.PENDING_ROLLBACK.name,
                userId = userId
            )
            if (itemId != null) {
                pipelineAtomReplaceItemDao.updateAtomReplaceItemByItemId(
                    dslContext = context,
                    itemId = itemId,
                    status = TaskStatusEnum.PENDING_ROLLBACK.name,
                    userId = userId
                )
            } else {
                pipelineAtomReplaceItemDao.updateAtomReplaceItemByBaseId(
                    dslContext = context,
                    baseId = baseId,
                    status = TaskStatusEnum.PENDING_ROLLBACK.name,
                    userId = userId
                )
            }
        }
        return Result(true)
    }

    fun getPipelineAtomRelList(
        userId: String,
        atomCode: String,
        version: String? = null,
        startUpdateTime: String,
        endUpdateTime: String,
        page: Int = 1,
        pageSize: Int = 10
    ): Result<Page<PipelineAtomRel>?> {
        // 判断用户是否有权限查询该插件的流水线信息
        validateUserAtomPermission(atomCode, userId)
        val convertStartUpdateTime = DateTimeUtil.stringToLocalDateTime(startUpdateTime)
        val convertEndUpdateTime = DateTimeUtil.stringToLocalDateTime(endUpdateTime)
        // 校验查询时间范围跨度
        validateQueryTimeRange(convertStartUpdateTime, convertEndUpdateTime)
        val pipelineAtomRelCount = pipelineModelTaskDao.countByAtomCode(
            dslContext = dslContext,
            atomCode = atomCode,
            version = version,
            startUpdateTime = convertStartUpdateTime,
            endUpdateTime = convertEndUpdateTime
        )
        val secrecyProjectSet: Set<String>? = getSecrecyProjectSet(pipelineAtomRelCount)
        // 查询使用该插件的流水线信息
        val pipelineAtomRelList =
            pipelineModelTaskDao.listByAtomCode(
                dslContext = dslContext,
                atomCode = atomCode,
                version = version,
                startUpdateTime = convertStartUpdateTime,
                endUpdateTime = convertEndUpdateTime,
                page = page,
                pageSize = pageSize
            )?.map { pipelineModelTask ->
                val pipelineId = pipelineModelTask[KEY_PIPELINE_ID] as String
                val projectId = pipelineModelTask[KEY_PROJECT_ID] as String
                val pipelineInfoRecord = pipelineInfoDao.getPipelineInfo(dslContext, projectId, pipelineId)
                val pipelineBuildSummaryRecord = pipelineBuildSummaryDao.get(dslContext, projectId, pipelineId)
                val secrecyFlag = secrecyProjectSet?.contains(projectId) == true
                val pipelineUrl = if (secrecyFlag) HIDDEN_SYMBOL else getPipelineUrl(projectId, pipelineId, false)
                PipelineAtomRel(
                    pipelineUrl = pipelineUrl,
                    atomVersion = pipelineModelTask[KEY_VERSION] as? String,
                    modifier = if (secrecyFlag) HIDDEN_SYMBOL else pipelineInfoRecord!!.lastModifyUser,
                    updateTime = DateTimeUtil.toDateTime(pipelineInfoRecord!!.updateTime),
                    executor = if (secrecyFlag) HIDDEN_SYMBOL else pipelineBuildSummaryRecord?.latestStartUser,
                    executeTime = DateTimeUtil.toDateTime(pipelineBuildSummaryRecord?.latestStartTime)
                )
            }
        val totalPages = PageUtil.calTotalPage(pageSize, pipelineAtomRelCount)
        return Result(
            Page(
                count = pipelineAtomRelCount,
                page = page,
                pageSize = pageSize,
                totalPages = totalPages,
                records = pipelineAtomRelList ?: listOf()
            )
        )
    }

    private fun getSecrecyProjectSet(count: Long): Set<String>? {
        var secrecyProjectSet: Set<String>? = null
        if (count > 0) {
            val listSecrecyProjectResult =
                client.get(ServiceProjectResource::class).listSecrecyProject()
            if (listSecrecyProjectResult.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = listSecrecyProjectResult.code.toString(),
                    defaultMessage = listSecrecyProjectResult.message
                )
            }
            secrecyProjectSet = listSecrecyProjectResult.data
        }
        return secrecyProjectSet
    }

    private fun validateQueryTimeRange(
        convertStartUpdateTime: LocalDateTime,
        convertEndUpdateTime: LocalDateTime
    ) {
        val tmpTime = convertStartUpdateTime.plusDays(maxRelQueryRangeTime)
        if (convertEndUpdateTime.isAfter(tmpTime)) {
            // 超过查询时间范围则报错
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_QUERY_TIME_RANGE_TOO_LARGE,
                params = arrayOf(maxRelQueryRangeTime.toString())
            )
        }
    }

    fun exportPipelineAtomRelCsv(
        userId: String,
        atomCode: String,
        version: String? = null,
        startUpdateTime: String,
        endUpdateTime: String,
        response: HttpServletResponse
    ) {
        // 判断用户是否有权限查询该插件的流水线信息
        validateUserAtomPermission(atomCode, userId)
        val convertStartUpdateTime = DateTimeUtil.stringToLocalDateTime(startUpdateTime)
        val convertEndUpdateTime = DateTimeUtil.stringToLocalDateTime(endUpdateTime)
        // 校验查询时间范围跨度
        validateQueryTimeRange(convertStartUpdateTime, convertEndUpdateTime)
        // 判断导出的流水线数量是否超过系统规定的最大值
        val pipelineAtomRelCount = pipelineModelTaskDao.countByAtomCode(
            dslContext = dslContext,
            atomCode = atomCode,
            version = version,
            startUpdateTime = convertStartUpdateTime,
            endUpdateTime = convertEndUpdateTime
        )
        if (pipelineAtomRelCount > maxRelQueryNum) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_QUERY_NUM_TOO_BIG,
                params = arrayOf(maxRelQueryNum.toString())
            )
        }
        val secrecyProjectSet: Set<String>? = getSecrecyProjectSet(pipelineAtomRelCount)
        val dataList = mutableListOf<Array<String?>>()
        var page = 1
        do {
            val pipelineAtomRelList = pipelineModelTaskDao.listByAtomCode(
                dslContext = dslContext,
                atomCode = atomCode,
                version = version,
                startUpdateTime = convertStartUpdateTime,
                endUpdateTime = convertEndUpdateTime,
                page = page,
                pageSize = DEFAULT_PAGE_SIZE
            )
            val pageDataList = mutableListOf<Array<String?>>()
            val pagePipelineIdSet = mutableSetOf<String>()
            pipelineAtomRelList?.forEach { pipelineAtomRel ->
                val pipelineId = pipelineAtomRel[KEY_PIPELINE_ID] as String
                val projectId = pipelineAtomRel[KEY_PROJECT_ID] as String
                val secrecyFlag = secrecyProjectSet?.contains(projectId) == true
                pagePipelineIdSet.add(pipelineId)
                val dataArray = arrayOfNulls<String>(7)
                dataArray[0] = if (secrecyFlag) HIDDEN_SYMBOL else getPipelineUrl(projectId, pipelineId, true)
                dataArray[1] = pipelineAtomRel[KEY_VERSION] as? String
                dataArray[6] = pipelineId
                pageDataList.add(dataArray)
            }
            if (pagePipelineIdSet.isNotEmpty()) {
                // 查询流水线基本信息
                val pagePipelineInfoRecordMap = pipelineInfoDao.listInfoByPipelineIds(
                    dslContext = dslContext,
                    pipelineIds = pagePipelineIdSet
                ).map { it.pipelineId to it }.toMap()
                for (index in pagePipelineIdSet.indices) {
                    val dataArray = pageDataList[index]
                    val pipelineId = dataArray[6]
                    val pipelineInfoRecord = pagePipelineInfoRecordMap[pipelineId]
                    val secrecyFlag = dataArray[0] == HIDDEN_SYMBOL
                    dataArray[2] = if (secrecyFlag) HIDDEN_SYMBOL else pipelineInfoRecord!!.lastModifyUser
                    dataArray[3] = DateTimeUtil.toDateTime(pipelineInfoRecord!!.updateTime)
                }
                // 查询流水线汇总信息
                val pagePipelineSummaryRecordMap = pipelineBuildSummaryDao.listSummaryByPipelineIds(
                    dslContext = dslContext,
                    pipelineIds = pagePipelineIdSet
                ).map { it.pipelineId to it }.toMap()
                for (index in pagePipelineIdSet.indices) {
                    val dataArray = pageDataList[index]
                    val pipelineId = dataArray[6]
                    val pipelineSummaryRecord = pagePipelineSummaryRecordMap[pipelineId]
                    val secrecyFlag = dataArray[0] == HIDDEN_SYMBOL
                    dataArray[4] = if (secrecyFlag) HIDDEN_SYMBOL else pipelineSummaryRecord!!.latestStartUser
                    dataArray[5] = DateTimeUtil.toDateTime(pipelineSummaryRecord!!.latestStartTime)
                    dataArray[6] = null
                }
            }
            dataList.addAll(pageDataList)
            page++
        } while (pipelineAtomRelList?.size == DEFAULT_PAGE_SIZE)
        val headers = arrayOf(
            I18nUtil.getCodeLanMessage(PIPELINE_URL),
            I18nUtil.getCodeLanMessage(VERSION),
            I18nUtil.getCodeLanMessage(LATEST_MODIFIER),
            I18nUtil.getCodeLanMessage(LATEST_UPDATE_TIME),
            I18nUtil.getCodeLanMessage(LATEST_EXECUTOR),
            I18nUtil.getCodeLanMessage(LATEST_EXECUTE_TIME)
        )
        val bytes = CsvUtil.writeCsv(headers, dataList)
        CsvUtil.setCsvResponse(atomCode, bytes, response)
    }

    private fun getPipelineUrl(projectId: String, pipelineId: String, addDomain: Boolean): String {
        val mf = MessageFormat(pipelineEditPath)
        val convertPath = mf.format(arrayOf(projectId, pipelineId))
        return if (addDomain) "${HomeHostUtil.innerServerHost()}/$convertPath" else "/$convertPath"
    }

    private fun validateUserAtomPermission(atomCode: String, userId: String) {
        val validateResult =
            client.get(ServiceStoreResource::class).isStoreMember(atomCode, StoreTypeEnum.ATOM, userId)
        if (validateResult.isNotOk()) {
            throw ErrorCodeException(
                errorCode = validateResult.status.toString(),
                defaultMessage = validateResult.message
            )
        } else if (validateResult.isOk() && validateResult.data == false) {
            throw ErrorCodeException(
                errorCode = GET_PIPELINE_ATOM_INFO_NO_PERMISSION,
                params = arrayOf(atomCode)
            )
        }
    }

    fun getPipelineAtomPropList(
        userId: String,
        projectId: String,
        pipelineId: String,
        checkPermission: Boolean = true
    ): Result<Map<String, AtomProp>?> {
        if (checkPermission) {
            val permission = AuthPermission.VIEW
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(
                        userId,
                        projectId,
                        permission.getI18n(I18nUtil.getLanguage(userId)),
                        pipelineId
                    )
                )
            )
        }
        val model = pipelineRepositoryService.getModel(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
            )
        // 获取流水线下插件标识集合
        val atomCodes = mutableSetOf<String>()
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.forEach { element ->
                    atomCodes.add(element.getAtomCode())
                }
            }
        }
        return client.get(ServiceAtomResource::class).getAtomProps(atomCodes)
    }
}

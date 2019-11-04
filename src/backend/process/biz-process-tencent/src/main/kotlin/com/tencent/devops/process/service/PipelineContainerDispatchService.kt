/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyDevCloudDispatchType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.exsi.ESXiDispatchType
import com.tencent.devops.common.pipeline.type.idc.IDCDispatchType
import com.tencent.devops.common.pipeline.type.pcg.PCGDispatchType
import com.tencent.devops.common.pipeline.type.tstack.TStackDispatchType
import com.tencent.devops.process.dao.PipelineContainerDispatchDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.exception.PipelineNotExistException
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineContainerDispatchInfo
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.util.exception.OldModelNotSupported
import com.tencent.devops.process.util.exception.UnknownContainerException
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch

@Component
class PipelineContainerDispatchService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineResDao: PipelineResDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineContainerDispatchDao: PipelineContainerDispatchDao,
    private val pipelineService: PipelineService
) {

    val logger = LoggerFactory.getLogger(javaClass)!!
    /**
     * 额外分表保存pipeline中Container的Dispatch信息以供统计查询
     */
    fun saveContainerDispatch(pipelineId: String, projectId: String) {
        logger.info("saveContainerDispatch|$pipelineId,$projectId")
        // 查出最新的pipelineVersion
        val pipelineInfo = pipelineInfoDao.getPipelineInfo(dslContext, pipelineId) ?: throw PipelineNotExistException("pipelineId=$pipelineId")
        val pipelineVersion = pipelineInfo.version
        // 首先根据事件信息查出Model
        val modelStr = pipelineResDao.getVersionModelString(dslContext, pipelineId, pipelineVersion)
        val model = JsonUtil.to(modelStr!!, object : TypeReference<Model>() {})
        // 同时往Model分出的子表中插入数据
        // 遍历stages
        val pipelineContainerDispatchInfoList = mutableListOf<PipelineContainerDispatchInfo>()
        model.stages.forEach { stage ->
            for (i in stage.containers.indices) {
                val container = stage.containers[i]
                val containerId = container.containerId
                if (container is TriggerContainer || container is NormalContainer) {
                    continue
                } else {
                    var dispatchBuildType = ""
                    var dispatchValue = ""
                    var dispatchImageType: String? = null
                    var dispatchCredentialId: String? = null
                    var dispatchWorkspace: String? = null
                    var dispatchAgentType: String? = null
                    when (container) {
                        is VMBuildContainer -> {
                            if (container.dispatchType == null) {
                                throw OldModelNotSupported("container.dispatchType does not exist")
                            }
                            dispatchBuildType = container.dispatchType!!.buildType().name
                            dispatchValue = container.dispatchType!!.value
                            when (container.dispatchType) {
                                is DockerDispatchType -> {
                                    val dispatchType = container.dispatchType as DockerDispatchType
                                    dispatchImageType = dispatchType.imageType?.name
                                    dispatchCredentialId = dispatchType.credentialId
                                }
                                is ThirdPartyAgentIDDispatchType -> {
                                    val dispatchType = container.dispatchType as ThirdPartyAgentIDDispatchType
                                    dispatchWorkspace = dispatchType.workspace
                                    dispatchAgentType = dispatchType.agentType.name
                                }
                                is ThirdPartyAgentEnvDispatchType -> {
                                    val dispatchType = container.dispatchType as ThirdPartyAgentEnvDispatchType
                                    dispatchWorkspace = dispatchType.workspace
                                    dispatchAgentType = dispatchType.agentType.name
                                }
                                is ThirdPartyDevCloudDispatchType -> {
                                    val dispatchType = container.dispatchType as ThirdPartyDevCloudDispatchType
                                    dispatchWorkspace = dispatchType.workspace
                                    dispatchAgentType = dispatchType.agentType.name
                                }
                                is IDCDispatchType -> {
                                    val dispatchType = container.dispatchType as IDCDispatchType
                                    dispatchImageType = dispatchType.imageType?.name
                                    dispatchCredentialId = dispatchType.credentialId
                                }
                                else -> {
                                }
                            }
                        }
                    }
                    pipelineContainerDispatchInfoList.add(
                        PipelineContainerDispatchInfo(
                            containerId = containerId!!,
                            pipelineId = pipelineId,
                            pipelineVersion = pipelineVersion,
                            projectId = projectId,
                            dispatchBuildType = dispatchBuildType,
                            dispatchValue = dispatchValue,
                            dispatchImageType = dispatchImageType,
                            dispatchCredentialId = dispatchCredentialId,
                            dispatchWorkspace = dispatchWorkspace,
                            dispatchAgentType = dispatchAgentType
                        )
                    )
                }
            }
        }
        pipelineContainerDispatchDao.batchCreate(dslContext, pipelineContainerDispatchInfoList)
    }

    fun deleteContainerDispatch(pipelineId: String) {
        // 无论软硬删除，都清空container信息
        pipelineContainerDispatchDao.delete(dslContext, pipelineId)
    }

    fun listPipelinesByDispatch(
        dispatchBuildType: String,
        dispatchValue: String?,
        channelCode: ChannelCode,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType
    ): Page<Pipeline> {
        logger.info("listPipelinesByDispatch|$dispatchBuildType,$dispatchValue,$page,$pageSize,$channelCode")
        var pipelines: List<Pipeline> = mutableListOf()
        var totalCount = 0
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            // 查出符合条件的记录总量
            totalCount = pipelineContainerDispatchDao.getPipelineNum(
                dslContext = context,
                dispatchBuildType = dispatchBuildType,
                dispatchValue = dispatchValue
            )
            // 查出使用指定构建资源的流水线与对应版本
            val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
            val records = pipelineContainerDispatchDao.listPipelineIds(
                dslContext = context,
                dispatchBuildType = dispatchBuildType,
                dispatchValue = dispatchValue,
                limit = sqlLimit.limit,
                offset = sqlLimit.offset
            )
            val pipelineIds = records!!.map {
                it.value1()
            }
            val pipelineIdsStr = pipelineIds.fold("") { s1, s2 -> "$s1:$s2" }
            logger.info("Inner:pipelineIds=[$pipelineIdsStr]")

            // 查所有途径创建的流水线中使用某镜像的流水线
            pipelines = pipelineService.listPipelinesByIds(null, pipelineIds.toSet())
        }
        // 排序
        val watch = StopWatch()
        watch.start("s_r_list_b_ps_sort")
        pipelineService.sortPipelines(pipelines, sortType)
        watch.stop()
        logger.info("Inner:listPipelinesByProjectIds:sort:watch=$watch")
        return Page(
            page = PageUtil.getValidPage(page),
            pageSize = PageUtil.getValidPageSize(pageSize),
            count = totalCount.toLong(),
            records = pipelines
        )
    }

    fun extractDispatchTypeByProjectId(
        userId: String,
        projectId: String,
        interfaceName: String? = "Anon interface"
    ): String {
        logger.info("extractDispatchTypeByProjectId|$userId,$projectId,$interfaceName")
        val pipelineInfoRecords = pipelineInfoDao.listPipelineInfoByProject(dslContext, projectId)
        val pipelineIds = pipelineInfoRecords?.map { it.pipelineId }?.toSet() ?: mutableSetOf()
        val pipelineResRecords = pipelineResDao.listModelResource(dslContext, pipelineIds)
        val mapper = jacksonObjectMapper()
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        pipelineResRecords.forEach { record ->
            var flag = false
            val pipelineId = record.pipelineId
            val pipelineVersion = record.version
            // 日志粒度：正常情况下一个pipeline+version一条日志，异常情况下打上下文
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                // 如果当前没有已拆分的dispatch数据才进行迁移
                if (!pipelineContainerDispatchDao.exists(context, pipelineId, pipelineVersion)) {
                    var model: Model
                    var log: String
                    try {
                        model = mapper.readValue(record.model)
                        saveModel(
                            dslContext = context,
                            pipelineId = pipelineId,
                            pipelineVersion = pipelineVersion,
                            model = model,
                            projectId = projectId,
                            interfaceName = interfaceName
                        )
                        flag = true
                        log = "ok"
                    } catch (e: JsonMappingException) {
                        log = "old,skip"
                    } catch (e: OldModelNotSupported) {
                        log = "old,skip"
                    }
                    logger.info("$interfaceName:$projectId:$pipelineId:$pipelineVersion:$log")
                    if (flag) {
                        // 迁移完一条休息5ms
                        Thread.sleep(5)
                    }
                } else {
                    logger.info("$interfaceName:$projectId:$pipelineId:$pipelineVersion:DataExists:skip")
                }
            }
        }
        return "success"
    }

    fun saveModel(
        dslContext: DSLContext,
        pipelineId: String,
        pipelineVersion: Int,
        model: Model,
        projectId: String,
        interfaceName: String? = "Anon interface"
    ) {
        val pipelineContainerDispatchInfoList = mutableListOf<PipelineContainerDispatchInfo>()
        model.stages.forEach { stage ->
            for (i in stage.containers.indices) {
                val container = stage.containers[i]
                val containerId = container.containerId
                if (container is TriggerContainer || container is NormalContainer) {
                    continue
                } else {
                    var dispatchBuildType: String
                    var dispatchValue: String
                    var dispatchImageType: String? = null
                    var dispatchCredentialId: String? = null
                    var dispatchWorkspace: String? = null
                    var dispatchAgentType: String? = null
                    when (container) {
                        is VMBuildContainer -> {
                            if (container.dispatchType == null) {
                                throw OldModelNotSupported("container.dispatchType does not exist")
                            }
                            dispatchBuildType = container.dispatchType!!.buildType().name
                            dispatchValue = container.dispatchType!!.value
                            when (container.dispatchType) {
                                is DockerDispatchType -> {
                                    val dispatchType = container.dispatchType as DockerDispatchType
                                    dispatchImageType = dispatchType.imageType?.name
                                    dispatchCredentialId = dispatchType.credentialId
                                }
                                is ESXiDispatchType -> {
                                }
                                is TStackDispatchType -> {
                                }
                                is ThirdPartyAgentIDDispatchType -> {
                                    val dispatchType = container.dispatchType as ThirdPartyAgentIDDispatchType
                                    dispatchWorkspace = dispatchType.workspace
                                    dispatchAgentType = dispatchType.agentType.name
                                }
                                is ThirdPartyAgentEnvDispatchType -> {
                                    val dispatchType = container.dispatchType as ThirdPartyAgentEnvDispatchType
                                    dispatchWorkspace = dispatchType.workspace
                                    dispatchAgentType = dispatchType.agentType.name
                                }
                                is PCGDispatchType -> {
                                }
                                is ThirdPartyDevCloudDispatchType -> {
                                    val dispatchType = container.dispatchType as ThirdPartyDevCloudDispatchType
                                    dispatchWorkspace = dispatchType.workspace
                                    dispatchAgentType = dispatchType.agentType.name
                                }
                                is PublicDevCloudDispathcType -> {
                                }
                                is IDCDispatchType -> {
                                    val dispatchType = container.dispatchType as IDCDispatchType
                                    dispatchImageType = dispatchType.imageType?.name
                                    dispatchCredentialId = dispatchType.credentialId
                                }
                                else -> {
                                    logger.info("$interfaceName:$projectId:$pipelineId:$pipelineVersion:warn:UnknownDispatchType, dispatchBuildType=$dispatchBuildType,dispatchValue=$dispatchValue:compatible")
                                }
                            }
                        }
                        else -> {
                            throw UnknownContainerException("$interfaceName:$projectId:$pipelineId:$pipelineVersion:UnknownContainer, containerStr=${JsonUtil.toJson(container)},modelStr=${JsonUtil.toJson(model)}")
                        }
                    }
                    pipelineContainerDispatchInfoList.add(
                        PipelineContainerDispatchInfo(
                            containerId = containerId!!,
                            pipelineId = pipelineId,
                            pipelineVersion = pipelineVersion,
                            projectId = projectId,
                            dispatchBuildType = dispatchBuildType,
                            dispatchValue = dispatchValue,
                            dispatchImageType = dispatchImageType,
                            dispatchCredentialId = dispatchCredentialId,
                            dispatchWorkspace = dispatchWorkspace,
                            dispatchAgentType = dispatchAgentType
                        )
                    )
                }
            }
        }
        pipelineContainerDispatchDao.batchCreate(dslContext, pipelineContainerDispatchInfoList)
    }
}
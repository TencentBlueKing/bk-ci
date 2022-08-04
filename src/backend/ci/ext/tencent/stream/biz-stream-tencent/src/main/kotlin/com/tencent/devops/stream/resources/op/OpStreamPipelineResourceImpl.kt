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

package com.tencent.devops.stream.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.ModelUpdate
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.stream.api.op.OpStreamPipelineResource
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.service.StreamPipelineBranchService
import com.tencent.devops.stream.trigger.service.StreamEventService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RestResource
class OpStreamPipelineResourceImpl @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val streamPipelineBranchService: StreamPipelineBranchService,
    private val streamEventService: StreamEventService,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val streamBasicSettingService: StreamBasicSettingService
) : OpStreamPipelineResource {

    private val logger = LoggerFactory.getLogger(OpStreamPipelineResourceImpl::class.java)

    override fun checkBranches(userId: String, gitProjectId: Long, pipelineId: String): Result<Boolean> {
        streamPipelineBranchService.deleteBranch(
            gitProjectId = gitProjectId,
            pipelineId = pipelineId,
            branch = null
        )
        pipelineResourceDao.deleteByPipelineId(dslContext, pipelineId)
        client.get(ServicePipelineResource::class).delete(
            userId = userId, projectId = "git_$gitProjectId", pipelineId = pipelineId,
            channelCode = ChannelCode.GIT
        )
        // 删除相关的构建记录
        streamEventService.deletePipelineBuildHistory(setOf(pipelineId))
        return Result(true)
    }

    override fun listJobIdConflict(startTime: Long?, endTime: Long?): Result<Int> {
        var startTimeTemp = startTime
        if (startTimeTemp == null) {
            startTimeTemp = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).timestampmilli()
        }
        var endTimeTemp = endTime
        if (endTimeTemp == null) {
            endTimeTemp = LocalDateTime.of(LocalDate.now(), LocalTime.MAX).timestampmilli()
        }
        val pipelineId2Yaml = gitRequestEventBuildDao.getLatestPipelineByDuration(
            dslContext = dslContext,
            startTime = startTimeTemp,
            endTime = endTimeTemp
        )
        logger.info("listJobIdConflict: \n$pipelineId2Yaml")
        return Result(pipelineId2Yaml.size)
    }

    override fun batchUpdateModelName(): String {
        val allPipeline = pipelineResourceDao.getAllPipeline(dslContext = dslContext).toMutableList()
        val gitProjectIdToBasicSetting = streamBasicSettingService.getBasicSettingRecordList(
            allPipeline.map { it.gitProjectId }
        ).associateBy { it.id }
        val failModelUpdates = mutableListOf<ModelUpdate>()
        val modelUpdateList = mutableListOf<ModelUpdate>()
        logger.info(
            "allPipelineCount:${allPipeline.size}," +
                "gitProjectIdToBasicSettingCount:${gitProjectIdToBasicSetting.size}"
        )
        while (allPipeline.isNotEmpty()) {
            while (allPipeline.isNotEmpty() && modelUpdateList.size < 100) {
                val first = allPipeline.removeFirst()
                val tGitBasicSetting = gitProjectIdToBasicSetting[first.gitProjectId] ?: continue
                modelUpdateList.add(
                    ModelUpdate(
                        name = first.displayName,
                        pipelineId = first.pipelineId,
                        projectId = tGitBasicSetting.projectCode,
                        updateUserId = tGitBasicSetting.enableUserId
                    )
                )
            }
            val failList = client.get(ServicePipelineResource::class).batchUpdateModelName(modelUpdateList).data
                ?: listOf()
            // 添加更新失败请求
            failModelUpdates.addAll(failList)
            // 清空下一轮继续
            modelUpdateList.clear()
        }
        return failModelFormat(failModelUpdates)
    }

    private fun failModelFormat(failModelUpdates: MutableList<ModelUpdate>): String {
        val sb = StringBuilder("pipelineId,projectId,userId,displayName,wrongMessage\n")
        failModelUpdates.forEach {
            sb.append(
                "${it.pipelineId},${it.projectId}," +
                    "${it.updateUserId},${it.name}," +
                    "${it.updateResultMessage}\n"
            )
        }
        return sb.toString()
    }
}

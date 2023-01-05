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
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.model.stream.tables.records.TGitRequestEventBuildRecord
import com.tencent.devops.process.yaml.v2.models.ScriptBuildYaml
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.stream.api.op.OpStreamCheckResource
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.service.StreamAsyncService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class OpStreamCheckResourceImpl @Autowired constructor(
    private val streamAsyncService: StreamAsyncService,
    private val dslContext: DSLContext,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao
) : OpStreamCheckResource {

    private val logger = LoggerFactory.getLogger(OpStreamCheckResource::class.java)

    override fun checkBranches(gitProjectId: Long?, pipelineId: String?): Result<Boolean> {
        streamAsyncService.checkPipelineBranch(gitProjectId, pipelineId)
        return Result(true)
    }

    override fun conflictJobs(buildDays: Long?): Result<String> {
        val conflictList = mutableListOf<TGitRequestEventBuildRecord>()
        var offset = 0
        do {
            val limit = 100
            val builds = gitRequestEventBuildDao.getBuildInLastDays(
                dslContext = dslContext,
                buildDays = buildDays ?: 1,
                offset = offset,
                limit = limit
            )
            builds.forEach nextRecord@{ record ->
                try {
                    val normalYaml = YamlUtil.getObjectMapper().readValue(
                        ScriptYmlUtils.formatYaml(record.normalizedYaml),
                        ScriptBuildYaml::class.java
                    )
                    val jobIdSet = mutableSetOf<String>()
                    normalYaml.stages.forEach { stage ->
                        stage.jobs.forEach { job ->
                            job.id?.let {
                                if (jobIdSet.contains(it)) {
                                    conflictList.add(record)
                                    return@nextRecord
                                } else jobIdSet.add(it)
                            }
                        }
                    }
                } catch (t: Throwable) {
                    logger.warn("OpStreamCheckResourceImpl|conflictJobs|${record.buildId}", t)
                }
            }
            offset += limit
        } while (offset < 100000)
        val sb = StringBuilder("buildId,pipelineId,triggerUser,createTime\n")
        conflictList.forEach { conflict ->
            sb.append(
                "${conflict.buildId},${conflict.pipelineId}," +
                    "${conflict.triggerUser},${conflict.createTime}\n"
            )
        }
        return Result(sb.toString())
    }
}

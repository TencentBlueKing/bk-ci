/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.opdata.service

import com.tencent.bkrepo.opdata.handler.QueryHandler
import com.tencent.bkrepo.opdata.model.ProjectModel
import com.tencent.bkrepo.opdata.model.RepoModel
import com.tencent.bkrepo.opdata.pojo.QueryRequest
import com.tencent.bkrepo.opdata.pojo.SearchRequest
import com.tencent.bkrepo.opdata.pojo.enums.Metrics
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

@Service
class GrafanaService @Autowired constructor(
    applicationContext: ApplicationContext,
    private val projectModel: ProjectModel,
    private val repoModel: RepoModel
) {
    private val handlerMap = mutableMapOf<Metrics, QueryHandler>()

    init {
        val handlers = applicationContext.getBeansOfType(QueryHandler::class.java).map { it.value }
        handlers.forEach {
            handlerMap[it.metric] = it
        }
    }

    fun search(request: SearchRequest): List<String> {
        val data = mutableListOf<String>()
        val target = if (request.target.isBlank()) Metrics.DEFAULT else Metrics.valueOf(request.target.split(":")[0])
        when (target) {
            Metrics.PROJECTIDLIST -> {
                data.addAll(projectModel.getProjectList().map { it.name })
            }
            Metrics.REPONAMELIST -> {
                val projectId = request.target.split(":")[1]
                data.addAll(repoModel.getRepoListByProjectId(projectId).map { it.name })
            }
            else -> {
                for (metric in Metrics.values()) {
                    data.add(metric.name)
                }
            }
        }
        return data
    }

    fun query(request: QueryRequest): List<Any> {
        val result = mutableListOf<Any>()
        request.targets.forEach {
            handlerMap[it.target]?.handle(it, result)
        }
        return result
    }
}

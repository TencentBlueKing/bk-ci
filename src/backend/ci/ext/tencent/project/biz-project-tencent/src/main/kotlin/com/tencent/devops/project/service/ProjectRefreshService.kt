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

package com.tencent.devops.project.service

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import java.util.concurrent.Executors
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectRefreshService @Autowired constructor(
    private val client: Client,
    private val projectService: ProjectService
) {
    private val executorService = Executors.newSingleThreadExecutor()

    fun refreshAllRelationAtomProjectProduct(userId: String): Boolean {
        executorService.execute {
            Thread.sleep(500)
            val startTime = System.currentTimeMillis()
            // 开始同步数据
            var page = PageUtil.DEFAULT_PAGE
            val pageSize = PageUtil.MAX_PAGE_SIZE
            var continueFlag = true
            while (continueFlag) {
                logger.info(
                    "refresh all relation atom project product page: $page , pageSize: $pageSize"
                )
                val gitProjectIds =
                    client.get(ServiceMarketAtomResource::class).getAtomRepositoryId(page, pageSize).data?.map {
                        "git_$it"
                    }
                if (gitProjectIds.isNullOrEmpty()) {
                    continueFlag = false
                    continue
                }
                gitProjectIds.let { projectService.batchUpdateProjectProductId(gitProjectIds, SYSTEM_DEFAULT_ID) }
                page++
            }
            logger.info("Syn all relation atom project product ${System.currentTimeMillis() - startTime}ms")
        }
        return true
    }

    companion object {
        private const val SYSTEM_DEFAULT_ID = 3238 // 蓝盾运营归属ID
        private val logger = LoggerFactory.getLogger(ProjectRefreshService::class.java)
    }
}

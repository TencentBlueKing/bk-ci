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

package com.tencent.devops.log.client

import com.tencent.devops.common.es.ESClient
import org.elasticsearch.client.Client
import org.slf4j.LoggerFactory

interface LogClient {

    fun admin(buildId: String) = getClient(buildId).admin()

    fun prepareBulk(buildId: String) = getClient(buildId).prepareBulk()

    fun prepareSearch(buildId: String, index: String) = getClient(buildId).prepareSearch()

    fun prepareMultiSearch(buildId: String) = getClient(buildId).prepareMultiSearch()

    fun prepareSearchScroll(buildId: String, scrollId: String) = getClient(buildId).prepareSearchScroll(buildId)

    fun prepareIndex(buildId: String, index: String, type: String) = getClient(buildId).prepareIndex(index, type)

    fun markESInactive(buildId: String) {
        // for the default implements just println the log
        val client = CurrentLogClient.getClient()
        if (client == null) {
            logger.warn("[$buildId] Fail to get the es client")
            return
        }
        logger.warn("[$buildId|${client.name}] Mark the es as inactive")
    }

    fun markESActive(buildId: String) {
        // for the default implement just println the log
        val esName = CurrentLogClient.getInactiveESName()
        logger.info("[$buildId|$esName] Mark the es as active")
    }

    private fun getClient(buildId: String): Client {
        val client = hashClient(buildId)
        CurrentLogClient.setClient(client)
        return client.client
    }

    fun getActiveClients(): List<ESClient>

    fun hashClient(buildId: String): ESClient

    companion object {
        private val logger = LoggerFactory.getLogger(LogClient::class.java)
    }
}
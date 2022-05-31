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

package com.tencent.devops.log.util

import com.tencent.devops.common.log.pojo.message.LogMessageWithLineNo
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.xcontent.XContentBuilder
import org.elasticsearch.xcontent.XContentFactory

object ESIndexUtils {

    fun getIndexSettings(shards: Int, replicas: Int, shardsPerNode: Int): Settings.Builder {
        return Settings.builder()
            .put("index.number_of_shards", shards)
            .put("index.number_of_replicas", replicas)
            .put("index.refresh_interval", "3s")
            .put("index.queries.cache.enabled", false)
            .put("index.routing.allocation.total_shards_per_node", shardsPerNode)
    }

    fun getTypeMappings(): XContentBuilder {
        return XContentFactory.jsonBuilder()
            .startObject()
            .startObject("properties")
            .startObject("buildId").field("type", "keyword").endObject()
            .startObject("timestamp").field("type", "long").endObject()
            .startObject("lineNo").field("type", "long").endObject()
            .startObject("tag").field("type", "keyword").endObject()
            .startObject("subTag").field("type", "keyword").endObject()
            .startObject("jobId").field("type", "keyword").endObject()
            .startObject("executeCount").field("type", "keyword").endObject()
            .startObject("logType").field("type", "text").endObject()
            .startObject("message").field("type", "text")
            .field("analyzer", "standard")
            .endObject()
            .endObject()
            .endObject()
    }

    fun getDocumentObject(
        buildId: String,
        logMessage: LogMessageWithLineNo
    ): XContentBuilder {
        return XContentFactory.jsonBuilder()
            .startObject()
            .field("buildId", buildId)
            .field("lineNo", logMessage.lineNo)
            .field("message", logMessage.message)
            .field("timestamp", logMessage.timestamp)
            .field("tag", logMessage.tag)
            .field("subTag", logMessage.subTag)
            .field("jobId", logMessage.jobId)
            .field("logType", logMessage.logType.name)
            .field("executeCount", logMessage.executeCount)
            .endObject()
    }
}

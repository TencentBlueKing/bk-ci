package com.tencent.devops.log.util

import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory

object ESIndexUtils {

    fun getIndexSettings(): Settings.Builder {
        return Settings.builder()
            .put("index.number_of_shards", 6)
            .put("index.number_of_replicas", 1)
            .put("index.refresh_interval", "3s")
            .put("index.queries.cache.enabled", false)
    }

    fun getTypeMappings(): XContentBuilder {
        return XContentFactory.jsonBuilder()
            .startObject()
            .startObject("properties")
            .startObject("buildId").field("type", "keyword").endObject()
            .startObject("timestamp").field("type", "long").endObject()
            .startObject("lineNo").field("type", "long").endObject()
            .startObject("tag").field("type", "keyword").endObject()
            .startObject("jobId").field("type", "keyword").endObject()
            .startObject("executeCount").field("type", "keyword").endObject()
            .startObject("logType").field("type", "text").endObject()
            .startObject("message").field("type", "text")
            .field("analyzer", "standard")
            .endObject()
            .endObject()
            .endObject()
    }
}
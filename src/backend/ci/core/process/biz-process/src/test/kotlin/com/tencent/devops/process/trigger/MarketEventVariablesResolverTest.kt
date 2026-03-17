package com.tencent.devops.process.trigger

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.process.trigger.market.MarketEventVariablesResolver
import com.tencent.devops.store.pojo.trigger.EventFieldMappingItem
import com.tencent.devops.store.pojo.trigger.enums.MappingSource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MarketEventVariablesResolverTest {

    val fieldMappings = listOf(
        EventFieldMappingItem(
            sourcePath = "X-Event-Type",
            targetField = "ci.event_type",
            source = MappingSource.HEADER
        ),
        EventFieldMappingItem(
            sourcePath = "X-TRACE-ID",
            targetField = "ci.event.trace_id",
            source = MappingSource.HEADER
        ),
        EventFieldMappingItem(
            sourcePath = "object_kind",
            targetField = "ci.event.object_kind",
            source = MappingSource.BODY
        ),
        EventFieldMappingItem(
            sourcePath = "object_attributes.target.name",
            targetField = "ci.event.target_name",
            source = MappingSource.BODY
        ),
        EventFieldMappingItem(
            sourcePath = "object_attributes.source.name",
            targetField = "ci.event.source_name",
            source = MappingSource.BODY
        ),
        EventFieldMappingItem(
            sourcePath = "object_attributes.target.http_url",
            targetField = "ci.event.target_http_url",
            source = MappingSource.BODY
        ),
        EventFieldMappingItem(
            sourcePath = "object_attributes.source.http_url",
            targetField = "ci.event.source_http_url",
            source = MappingSource.BODY
        ),
        EventFieldMappingItem(
            sourcePath = "merge_type",
            targetField = "ci.event.merge_type",
            source = MappingSource.BODY
        ),
        EventFieldMappingItem(
            sourcePath = "object_attributes.target_project_id",
            targetField = "ci.event.target_project_id",
            source = MappingSource.BODY
        ),
        EventFieldMappingItem(
            sourcePath = "object_attributes.labels",
            targetField = "ci.event.labels",
            source = MappingSource.BODY
        )
    )

    val headers = mapOf(
        "X-Event-Type" to "merge_request",
        "X-TRACE-ID" to "1234567890",
        "X-Source-ID" to "1234"
    )

    val body = "{\n" +
            "    \"object_kind\": \"merge_request\",\n" +
            "    \"manual_unlock\": false,\n" +
            "    \"user\": {\n" +
            "        \"name\": \"xiaoming\",\n" +
            "        \"username\": \"xiaoming\",\n" +
            "        \"avatar_url\": null\n" +
            "    },\n" +
            "    \"object_attributes\": {\n" +
            "        \"id\": 21147820,\n" +
            "        \"target_branch\": \"master\",\n" +
            "        \"source_branch\": \"feat_11884\",\n" +
            "        \"source_project_id\": 1064025,\n" +
            "        \"author_id\": 286464,\n" +
            "        \"assignee_id\": null,\n" +
            "        \"title\": \"perf: 优化流水线webhook触发流程 #11884\",\n" +
            "        \"created_at\": \"2025-08-22T10:13:36+0000\",\n" +
            "        \"updated_at\": \"2025-12-08T03:45:33+0000\",\n" +
            "        \"st_commits\": null,\n" +
            "        \"st_diffs\": null,\n" +
            "        \"milestone_id\": null,\n" +
            "        \"state\": \"reopened\",\n" +
            "        \"merge_status\": \"cannot_be_merged\",\n" +
            "        \"target_project_id\": 1064025,\n" +
            "        \"iid\": 166,\n" +
            "        \"description\": \"\",\n" +
            "        \"source\": {\n" +
            "            \"name\": \"HelloeWorlds_HJ\",\n" +
            "            \"ssh_url\": \"git@git.example.com:bkdevops-plugins/HelloeWorlds_HJ.git\",\n" +
            "            \"http_url\": \"http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ.git\",\n" +
            "            \"web_url\": \"http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ\",\n" +
            "            \"namespace\": \"bkdevops-plugins/HelloeWorlds_HJ\",\n" +
            "            \"visibility_level\": 10\n" +
            "        },\n" +
            "        \"target\": {\n" +
            "            \"name\": \"HelloeWorlds_HJ\",\n" +
            "            \"ssh_url\": \"git@git.example.com:bkdevops-plugins/HelloeWorlds_HJ.git\",\n" +
            "            \"http_url\": \"http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ.git\",\n" +
            "            \"web_url\": \"http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ\",\n" +
            "            \"namespace\": \"bkdevops-plugins/HelloeWorlds_HJ\",\n" +
            "            \"visibility_level\": 10\n" +
            "        },\n" +
            "        \"last_commit\": {\n" +
            "            \"id\": \"8435f653e6d0afdbaaefc7564c3a321bd348a140\",\n" +
            "            \"message\": \"Update b-c8bfb1e870c640daa8ee99af5b14b1a4.txt\",\n" +
            "            \"timestamp\": \"2025-10-31T12:02:42+0000\",\n" +
            "            \"url\": \"http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ/commits/8435f653e6d0afdbaaefc7564c3a321bd348a140\",\n" +
            "            \"author\": {\n" +
            "                \"name\": \"xiaoming\",\n" +
            "                \"email\": \"xiaoming@example.com\"\n" +
            "            }\n" +
            "        },\n" +
            "        \"merge_type\": null,\n" +
            "        \"merge_commit_sha\": null,\n" +
            "        \"before\": null,\n" +
            "        \"after\": null,\n" +
            "        \"url\": \"http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ/merge_requests/166\",\n" +
            "        \"action\": \"reopen\",\n" +
            "        \"extension_action\": \"reopen\",\n" +
            "        \"labels\": [\n" +
            "            \"for test\",\n" +
            "            \"tested\",\n" +
            "            \"service/process\"\n" +
            "        ]\n" +
            "    }\n" +
            "}".trimIndent()

    val marketEventVariablesResolver = MarketEventVariablesResolver()

    @Test
    fun getEventVariables() {
        val variables = marketEventVariablesResolver.getEventVariables(
            fieldMappings = fieldMappings,
            incomingHeaders = headers,
            incomingQueryParamMap = null,
            incomingBody = JsonUtil.toMap(body)
        )
        Assertions.assertEquals("merge_request", variables["ci.event_type"])
        Assertions.assertEquals("1234567890", variables["ci.event.trace_id"])
        Assertions.assertEquals("merge_request", variables["ci.event.object_kind"])
        Assertions.assertEquals("HelloeWorlds_HJ", variables["ci.event.target_name"])
        Assertions.assertEquals("HelloeWorlds_HJ", variables["ci.event.source_name"])
        Assertions.assertEquals(
            "http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ.git",
            variables["ci.event.target_http_url"]
        )
        Assertions.assertEquals(
            "http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ.git",
            variables["ci.event.source_http_url"]
        )
        Assertions.assertEquals(1064025, variables["ci.event.target_project_id"])
    }
}

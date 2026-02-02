package com.tencent.devops.process.trigger

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

    val body = """
        {
          "object_kind": "merge_request",
          "manual_unlock": false,
          "user": {
            "name": "xiaoming",
            "username": "xiaoming",
            "avatar_url": null
          },
          "object_attributes": {
            "id": 21147820,
            "target_branch": "master",
            "source_branch": "feat_11884",
            "source_project_id": 1064025,
            "author_id": 286464,
            "assignee_id": null,
            "title": "perf: 优化流水线webhook触发流程 #11884",
            "created_at": "2025-08-22T10:13:36+0000",
            "updated_at": "2025-12-08T03:45:33+0000",
            "st_commits": null,
            "st_diffs": null,
            "milestone_id": null,
            "state": "reopened",
            "merge_status": "cannot_be_merged",
            "target_project_id": 1064025,
            "iid": 166,
            "description": "",
            "source": {
              "name": "HelloeWorlds_HJ",
              "ssh_url": "git@git.example.com:bkdevops-plugins/HelloeWorlds_HJ.git",
              "http_url": "http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ.git",
              "web_url": "http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ",
              "namespace": "bkdevops-plugins/HelloeWorlds_HJ",
              "visibility_level": 10
            },
            "target": {
              "name": "HelloeWorlds_HJ",
              "ssh_url": "git@git.example.com:bkdevops-plugins/HelloeWorlds_HJ.git",
              "http_url": "http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ.git",
              "web_url": "http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ",
              "namespace": "bkdevops-plugins/HelloeWorlds_HJ",
              "visibility_level": 10
            },
            "last_commit": {
              "id": "8435f653e6d0afdbaaefc7564c3a321bd348a140",
              "message": "Update b-c8bfb1e870c640daa8ee99af5b14b1a4.txt",
              "timestamp": "2025-10-31T12:02:42+0000",
              "url": "http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ/commits/8435f653e6d0afdbaaefc7564c3a321bd348a140",
              "author": {
                "name": "xiaoming",
                "email": "xiaoming@example.com"
              }
            },
            "merge_type": null,
            "merge_commit_sha": null,
            "before": null,
            "after": null,
            "url": "http://git.example.com/bkdevops-plugins/HelloeWorlds_HJ/merge_requests/166",
            "action": "reopen",
            "extension_action": "reopen",
            "labels": ["for test", "tested", "service/process"]
          }
        }
    """.trimIndent()

    val marketEventVariablesResolver = MarketEventVariablesResolver()

    @Test
    fun getEventVariables() {
        val variables = marketEventVariablesResolver.getEventVariables(
            fieldMappings = fieldMappings,
            incomingHeaders = headers,
            incomingQueryParamMap = null,
            incomingBody = body
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

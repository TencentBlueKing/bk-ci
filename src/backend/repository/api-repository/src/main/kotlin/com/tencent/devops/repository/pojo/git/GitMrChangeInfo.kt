package com.tencent.devops.repository.pojo.git

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel

/*
* {
    "labels": [],
    "id": 683474,
    "title": "--story=855977085 【openapi】获取构建详情加上构建变量列表",
    "target_project_id": 88710,
    "target_branch": "integration",
    "source_project_id": 88710,
    "source_branch": "story_855977085",
    "state": "opened",
    "merge_status": "unchecked",
    "iid": 1438,
    "description": "--story=855977085 【openapi】获取构建详情加上构建变量列表",
    "created_at": "2019-09-19T13:53:20+0000",
    "updated_at": "2019-09-19T14:11:54+0000",
    "assignee": null,
    "author": {
        "id": 23906,
        "username": "jsonwan",
        "web_url": "http://git.code.oa.com/u/jsonwan",
        "name": "jsonwan",
        "state": "active",
        "avatar_url": "http://git.code.oa.com/assets/images/avatar/no_user_avatar.png"
    },
    "milestone": null,
    "necessary_reviewers": null,
    "suggestion_reviewers": null,
    "files": [
        {
            "old_path": "service/service-process/src/main/kotlin/com/tencent/devops/process/resources/ServiceBuildResourceImpl.kt",
            "new_path": "service/service-process/src/main/kotlin/com/tencent/devops/process/resources/ServiceBuildResourceImpl.kt",
            "a_mode": 33188,
            "b_mode": 33188,
            "diff": "",
            "new_file": false,
            "renamed_file": false,
            "deleted_file": false,
            "is_too_large": false,
            "is_collapse": true,
            "additions": 217,
            "deletions": 36
        },
        {
            "old_path": "service/service-process/src/main/kotlin/com/tencent/devops/process/engine/service/PipelineBuildService.kt",
            "new_path": "service/service-process/src/main/kotlin/com/tencent/devops/process/engine/service/PipelineBuildService.kt",
            "a_mode": 33188,
            "b_mode": 33188,
            "diff": "@@ -52,6 +52,7 @@\n ipelineRuntimeService.    projectId: String,\n",
            "new_file": false,
            "renamed_file": false,
            "deleted_file": false,
            "is_too_large": false,
            "is_collapse": false,
            "additions": 57,
            "deletions": 5
        },
        {
            "old_path": "service/service-openapi/src/main/kotlin/com/tencent/devops/openapi/resources/OpenApiBuildResourceImpl.kt",
            "new_path": "service/service-openapi/src/main/kotlin/com/tencent/devops/openapi/resources/OpenApiBuildResourceImpl.kt",
            "a_mode": 33188,
            "b_mode": 33188,
            "diff": "@@ -1,15 +1,15 @@\n package openapt {\n",
            "new_file": false,
            "renamed_file": false,
            "deleted_file": false,
            "is_too_large": false,
            "is_collapse": false,
            "additions": 20,
            "deletions": 16
        },
        {
            "old_path": "service/service-openapi/src/main/kotlin/com/tencent/devops/openapi/resources/ApigwBuildResourceImpl.kt",
            "new_path": "service/service-openapi/src/main/kotlin/com/tencent/devops/openapi/resources/ApigwBuildResourceImpl.kt",
            "a_mode": 33188,
            "b_mode": 33188,
            "diff": "@@ -8,6 +8,7 @@\n import LoggerFactory.getLogger(ApigwBuildResourceImpl::class.java)\n     }\n",
            "new_file": false,
            "renamed_file": false,
            "deleted_file": false,
            "is_too_large": false,
            "is_collapse": false,
            "additions": 36,
            "deletions": 26
        },
        {
            "old_path": "/dev/null",
            "new_path": "api/api-process/src/main/kotlin/com/tencent/devops/process/pojo/BuildHistoryWithVars.kt",
            "a_mode": 33188,
            "b_mode": 0,
            "diff": "@@ -0,0 +1,59 @@\n+package process.pojo\n+\n+import artifacf file\n",
            "new_file": true,
            "renamed_file": false,
            "deleted_file": false,
            "is_too_large": false,
            "is_collapse": false,
            "additions": 59,
            "deletions": 0
        },
        {
            "old_path": "api/api-process/src/main/kotlin/com/tencent/devops/process/api/ServiceBuildResource.kt",
            "new_path": "api/api-process/src/main/kotlin/com/tencent/devops/process/api/ServiceBuildResource.kt",
            "a_mode": 33188,
            "b_mode": 33188,
            "diff": "@@ -10,6 +10,7 @@\n import process.pojo.B @Api    @GET\n",
            "new_file": false,
            "renamed_file": false,
            "deleted_file": false,
            "is_too_large": false,
            "is_collapse": false,
            "additions": 2,
            "deletions": 1
        },
        {
            "old_path": "api/api-openapi/src/main/kotlin/com/tencent/devops/openapi/OpenApiBuildResource.kt",
            "new_path": "api/api-openapi/src/main/kotlin/com/tencent/devops/openapi/OpenApiBuildResource.kt",
            "a_mode": 33188,
            "b_mode": 33188,
            "diff": "@@ -3,7 +3,7 @@\n import common.api.auth.AUTH_HEADER_DEVOPS_USER_ID\n\\ No newline at end of file\n",
            "new_file": false,
            "renamed_file": false,
            "deleted_file": false,
            "is_too_large": false,
            "is_collapse": false,
            "additions": 2,
            "deletions": 2
        },
        {
            "old_path": "api/api-openapi/src/main/kotlin/com/tencent/devops/openapi/ApigwBuildResource.kt",
            "new_path": "api/api-openapi/src/main/kotlin/com/tencent/devops/openapi/ApigwBuildResource.kt",
            "a_mode": 33188,
            "b_mode": 33188,
            "diff": "@@ -5,6 +5,7 @@\n import      @GET\n",
            "new_file": false,
            "renamed_file": false,
            "deleted_file": false,
            "is_too_large": false,
            "is_collapse": false,
            "additions": 2,
            "deletions": 1
        }
    ],
    "upvotes": 0,
    "downvotes": 0,
    "project_id": 88710,
    "work_in_progress": false
}
* */

@ApiModel("git mr文件变更信息")
data class GitMrChangeInfo(
    val title: String = "",
    @JsonProperty("target_project_id")
    val targetProjectId: String = "",
    @JsonProperty("target_branch")
    val targetBranch: String? = "",
    @JsonProperty("source_project_id")
    val sourceProjectId: String? = "",
    @JsonProperty("source_branch")
    val sourceBranch: String? = "",
    @JsonProperty("created_at")
    val createTime: String? = "",
    @JsonProperty("updated_at")
    val updateTime: String? = "",
    @JsonProperty("iid")
    val mrNumber: String = "",
    @JsonProperty("id")
    val mrId: String = "",
    val labels: List<String>,
    val description: String? = "",
    val files: List<GitMrFile>
) {
    data class GitMrFile(
        @JsonProperty("old_path")
        val oldPath: String,
        @JsonProperty("new_path")
        val newPath: String,
        @JsonProperty("new_file")
        val newFile: Boolean,
        @JsonProperty("renamed_file")
        val renameFile: Boolean,
        @JsonProperty("deleted_file")
        val deletedFile: Boolean,
        @JsonProperty("is_too_large")
        val isTooLarge: Boolean,
        @JsonProperty("additions")
        val additions: Int,
        @JsonProperty("deletions")
        val deletions: Int
    )
}

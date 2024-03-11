package com.tencent.devops.scm.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
{
"id": 25123,
"name": "test00",
"path": "test00",
"web_url": "https://git.xxx.com/groups/test00",
"description": "xxx",
"avatar_url": null,
"projects": [
{
"id": 64612,
"description": "0711",
"public": false,
"archived": false,
"visibility_level": 0,
"namespace": {
"id": 25123,
"name": "test00",
"path": "test00",
"web_url": "https://git.xxx.com/groups/test00",
"description": "xxx",
"avatar_url": null
},
"name": "test1",
"name_with_namespace": "test00/test1",
"path": "test1",
"path_with_namespace": "test00/test1",
"default_branch": "master",
"ssh_url_to_repo": "git@git.xxxxx:test00/test1.git",
"http_url_to_repo": "http://git.xxxxx.com/test00/test1.git",
"https_url_to_repo": "https://git.xxxxx.com/test00/test1.git",
"web_url": "https://git.xxxxx.com/test00/test1",
"tag_list": [],
"issues_enabled": true,
"merge_requests_enabled": true,
"wiki_enabled": true,
"snippets_enabled": true,
"review_enabled": true,
"fork_enabled": false,
"tag_name_regex": null,
"tag_create_push_level": 30,
"created_at": "2017-07-11T06:29:24+0000",
"last_activity_at": "2017-07-11T06:46:33+0000",
"creator_id": 18123,
"avatar_url": "https://git.xxxxx.com/uploads/project/avatar/123",
"watchs_count": 2,
"stars_count": 0,
"forks_count": 0,
"config_storage": {
"limit_lfs_file_size": 500,
"limit_size": 100000,
"limit_file_size": 100000,
"limit_lfs_size": 100000
},
"forked_from_project": "Forked Project not found",
"statistics": {
"commit_count": 3,
"repository_size": 0.004
}
},
"sub_projects": [{
"id": 66062,
"description": "gggg",
"public": false,
"archived": false,
"visibility_level": 0,
"namespace": {
"id": 25123,
"name": "test00",
"path": "test00",
"web_url": "https://git.xxxxx.com/groups/test00",
"description": "xxx",
"avatar_url": null
},
"name": "test02",
"name_with_namespace": "test00/test02",
"path": "test02",
"path_with_namespace": "test00/test02",
"default_branch": "master",
"ssh_url_to_repo": "git@git.xxxxx.com:test00/test02.git",
"http_url_to_repo": "http://git.xxxxx.com/test00/test02.git",
"https_url_to_repo": "https://git.xxxxx.com/test00/test02.git",
"web_url": "https://git.xxxxx.com/test00/test02",
"tag_list": [],
"issues_enabled": true,
"merge_requests_enabled": true,
"wiki_enabled": true,
"snippets_enabled": true,
"review_enabled": true,
"fork_enabled": false,
"tag_name_regex": null,
"tag_create_push_level": 30,
"created_at": "2018-07-19T07:13:22+0000",
"last_activity_at": "2018-07-19T07:13:23+0000",
"creator_id": 18604,
"avatar_url": "https://git.xxxxx.com/uploads/project/avatar/66062",
"watchs_count": 2,
"stars_count": 0,
"forks_count": 0,
"config_storage": {
"limit_lfs_file_size": 500,
"limit_size": 100000,
"limit_file_size": 100000,
"limit_lfs_size": 100000
},
"forked_from_project": "Forked Project not found",
"statistics": {
"commit_count": 0,
"repository_size": 0
}
}]
]
}
 */

@Schema(title = "git 项目组信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitProjectGroupInfo(
    val id: Long,
    val name: String,
    val path: String,
    val projects: List<GitProjectGroupProject>,
    @JsonProperty("sub_projects")
    val subProjects: List<GitProjectGroupProject>?
)

@Schema(title = "git 项目组项目信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitProjectGroupProject(
    val id: String
)

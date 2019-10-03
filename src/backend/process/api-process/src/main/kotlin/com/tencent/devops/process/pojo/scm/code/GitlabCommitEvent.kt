package com.tencent.devops.process.pojo.scm.code

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * {
　　"object_kind":"push",
　　"event_name":"push",
　　"before":"fe19454a8e03c7daa47d7fbf176f3cf62487f0ef",
　　"after":"f60e4eccb2a20610c4e8d43784791060b35c2690",
　　"ref":"refs/heads/4.x",
　　"checkout_sha":"f60e4eccb2a20610c4e8d43784791060b35c2690",
　　"message":null,
　　"user_id":138,
　　"user_name":"rdeng",
　　"user_username":"rdeng",
　　"user_email":"rdeng@tencent.com",
　　"user_avatar":null,
　　"project_id":477,
　　"project":{
　　　　"name":"soda-backend",
　　　　"description":"蓝盾客户端构建后端工程",
　　　　"web_url":"http://gitlab-paas.open.oa.com/devops/soda-backend",
　　　　"avatar_url":null,
　　　　"git_ssh_url":"git@gitlab-paas.open.oa.com:devops/soda-backend.git",
　　　　"git_http_url":"http://gitlab-paas.open.oa.com/devops/soda-backend.git",
　　　　"namespace":"devops",
　　　　"visibility_level":0,
　　　　"path_with_namespace":"devops/soda-backend",
　　　　"default_branch":"master",
　　　　"ci_config_path":null,
　　　　"homepage":"http://gitlab-paas.open.oa.com/devops/soda-backend",
　　　　"url":"git@gitlab-paas.open.oa.com:devops/soda-backend.git",
　　　　"ssh_url":"git@gitlab-paas.open.oa.com:devops/soda-backend.git",
　　　　"http_url":"http://gitlab-paas.open.oa.com/devops/soda-backend.git"
　　},
　　"commits":[
　　　　{
　　　　　　"id":"5aaf1189b29f1ede1a3118461acf61ebbc27a22b",
　　　　　　"message":"Update svn web hook logic",
　　　　　　"timestamp":"2018-03-16T16:44:23+08:00",
　　　　　　"url":"http://gitlab-paas.open.oa.com/devops/soda-backend/commit/5aaf1189b29f1ede1a3118461acf61ebbc27a22b",
　　　　　　"author":{
　　　　　　　　"name":"rdeng",
　　　　　　　　"email":"rdeng@tencent.com"
　　　　　　},
　　　　　　"added":[
　　　　　　　　"api/api-process/src/main/kotlin/com/tencent/soda/process/api/ExternalScmResource.kt",
　　　　　　　　"api/api-process/src/main/kotlin/com/tencent/soda/process/pojo/scm/code/ScmWebhookMatcher.kt",
　　　　　　　　"api/api-process/src/main/kotlin/com/tencent/soda/process/pojo/scm/code/git/GitCommitEvent.kt",
　　　　　　　　"api/api-process/src/main/kotlin/com/tencent/soda/process/pojo/scm/code/svn/PostCommitEvent.kt",
　　　　　　　　"api/api-process/src/main/kotlin/com/tencent/soda/process/pojo/scm/code/svn/PostCommitEventFile.kt",
　　　　　　　　"service/service-process/src/main/kotlin/com/tencent/soda/process/resources/ExternalScmResourceImpl.kt"
　　　　　　],
　　　　　　"modified":[
　　　　　　　　"common/common-pipeline/src/main/kotlin/com/tencent/soda/common/pipeline/pojo/element/trigger/CodeGitWebHookTriggerElement.kt",
　　　　　　　　"common/common-pipeline/src/main/kotlin/com/tencent/soda/common/pipeline/pojo/element/trigger/CodeGitlabWebHookTriggerElement.kt",
　　　　　　　　"service/service-process/src/main/kotlin/com/tencent/soda/process/service/BuildService.kt",
　　　　　　　　"service/service-process/src/main/kotlin/com/tencent/soda/process/service/ScmService.kt",
　　　　　　　　"service/service-process/src/main/kotlin/com/tencent/soda/process/service/VMBuildService.kt"
　　　　　　],
　　　　　　"removed":[

　　　　　　]
　　　　}
　　],
　　"total_commits_count":3,
　　"repository":{
　　　　"name":"soda-backend",
　　　　"url":"git@gitlab-paas.open.oa.com:devops/soda-backend.git",
　　　　"description":"蓝盾客户端构建后端工程",
　　　　"homepage":"http://gitlab-paas.open.oa.com/devops/soda-backend",
　　　　"git_http_url":"http://gitlab-paas.open.oa.com/devops/soda-backend.git",
　　　　"git_ssh_url":"git@gitlab-paas.open.oa.com:devops/soda-backend.git",
　　　　"visibility_level":0
　　}
}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabCommitEvent(
    val object_kind: String,
    val before: String,
    val after: String,
    val ref: String,
    val checkout_sha: String,
    val user_name: String,
    val project_id: Long,
    val project: GitlabCommitProject,
    val commits: List<GitlabCommit>,
    val total_commits_count: Int,
    val repository: GitlabCommitRepository
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabCommitProject(
    val name: String,
    val web_url: String,
    val git_ssh_url: String,
    val git_http_url: String,
    val path_with_namespace: String,
    val url: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabCommit(
    val id: String,
    val message: String,
    val timestamp: String,
    val url: String,
    val author: GitlabCommitAuthor
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabCommitAuthor(
    val name: String,
    val email: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitlabCommitRepository(
    val name: String,
    val url: String,
    val homepage: String,
    val git_http_url: String,
    val git_ssh_url: String
)
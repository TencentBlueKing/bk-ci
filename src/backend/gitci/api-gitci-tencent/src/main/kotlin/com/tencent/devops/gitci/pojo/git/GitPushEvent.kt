package com.tencent.devops.gitci.pojo.git

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.gitci.OBJECT_KIND_PUSH

/**
 * {
 *   "object_kind":"push",
 *   "before":"9d1861bd3ae32cda2a92479962712065aae19cf2",
 *   "after":"47f4f77f5428eed4e75f4d84d1b9089b38c5a34e",
 *   "ref":"refs/heads/test",
 *   "checkout_sha":"47f4f77f5428eed4e75f4d84d1b9089b38c5a34e",
 *   "user_name":"rdeng",
 *   "user_id":11648,
 *   "user_email":"rdeng@tencent.com",
 *   "project_id":46619,
 *   "repository":{
 *    　"name":"maven-hello-world",
 *      "description":"",
 *      "homepage":"http://git.code.oa.com/rdeng/maven-hello-world",
 *      "git_http_url":"http://git.code.oa.com/rdeng/maven-hello-world.git",
 *      "git_ssh_url":"git@git.code.oa.com:rdeng/maven-hello-world.git",
 *      "url":"git@git.code.oa.com:rdeng/maven-hello-world.git",
 *      "visibility_level":0
 *    },
 *    "commits":[
 *       {
 *         "id":"47f4f77f5428eed4e75f4d84d1b9089b38c5a34e",
 *         "message":"Test webhook",
 *         "timestamp":"2018-03-16T06:50:11+0000",
 *         "url":"http://git.code.oa.com/rdeng/maven-hello-world/commit/47f4f77f5428eed4e75f4d84d1b9089b38c5a34e",
 *         "author":{
 *           "name":"rdeng",
 *           "email":"rdeng@tencent.com"
 *         },
 *         "added":[
 *
 *         ],
 *         "modified":[
 *           "test.txt"
 *         ],
 *         "removed":[
 *         ]
 *       }
 *     ],
 *   "total_commits_count":1
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitPushEvent(
    val operation_kind: String,
    val ref: String,
    val before: String,
    val after: String,
    val user_name: String,
    val checkout_sha: String?,
    val project_id: Long,
    val repository: GitCommitRepository,
    val commits: List<GitCommit>,
    val total_commits_count: Int
) : GitEvent() {
    companion object {
        const val classType = OBJECT_KIND_PUSH
    }
}
package com.tencent.devops.scm.code.git.api

data class GitMRComment(
    val id: String,
    val body: String,
    val attachment: String?,
    val author: Author,
    val created_at: String,
    val system: Boolean
) {

    data class Author(
        val id: Long,
        val username: String,
        val web_url: String,
        val name: String,
        val state: String,
        val avatar_url: String
    )
}

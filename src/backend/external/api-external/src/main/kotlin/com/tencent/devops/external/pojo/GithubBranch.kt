package com.tencent.devops.external.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * {
    "name": "master",
    "commit": {
        "sha": "7fd1a60b01f91b314f59955a4e4d4e80d8edf11d",
        "node_id": "MDY6Q29tbWl0N2ZkMWE2MGIwMWY5MWIzMTRmNTk5NTVhNGU0ZDRlODBkOGVkZjExZA==",
        "commit": {
            "author": {
                "name": "The Octocat",
                "date": "2012-03-06T15:06:50-08:00",
                "email": "octocat@nowhere.com"
            },
            "url": "https://api.github.com/repos/octocat/Hello-World/git/commits/7fd1a60b01f91b314f59955a4e4d4e80d8edf11d",
            "message": "Merge pull request #6 from Spaceghost/patch-1\n\nNew line at end of file.",
            "tree": {
                "sha": "b4eecafa9be2f2006ce1b709d6857b07069b4608",
                "url": "https://api.github.com/repos/octocat/Hello-World/git/trees/b4eecafa9be2f2006ce1b709d6857b07069b4608"
            },
            "committer": {
                "name": "The Octocat",
                "date": "2012-03-06T15:06:50-08:00",
                "email": "octocat@nowhere.com"
            },
            "verification": {
                "verified": false,
                "reason": "unsigned",
                "signature": null,
                "payload": null
            }
        }
    }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubBranch(
    val name: String,
    val commit: GithubCommit?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubCommit(
    val sha: String,
    val node_id: String,
    val commit: GithubCommitData?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubCommitData(
    val message: String,
    val author: GithubCommitAuthor
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubCommitAuthor(
    val name: String,
    val date: String,
    val email: String
)

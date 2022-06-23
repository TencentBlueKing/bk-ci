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

package com.tencent.devops.stream.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.sdk.github.response.CommitResponse
import com.tencent.devops.scm.pojo.Commit
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("git 查询代码库项目信息| USER 使用")
@JsonIgnoreProperties(ignoreUnknown = true)
data class StreamCommitInfo(
    @JsonProperty("author_email")
    @ApiModelProperty(name = "author_email")
    val authorEmail: String?,
    @JsonProperty("author_name")
    @ApiModelProperty(name = "author_name")
    val authorName: String?,
    @JsonProperty("authored_date")
    @ApiModelProperty(name = "authored_date")
    val authoredDate: String?,
    @JsonProperty("committed_date")
    @ApiModelProperty(name = "committed_date")
    val committedDate: String?,
    @JsonProperty("committer_email")
    @ApiModelProperty(name = "committer_email")
    val committerEmail: String?,
    @JsonProperty("committer_name")
    @ApiModelProperty(name = "committer_name")
    val committerName: String?,
    @JsonProperty("created_at")
    @ApiModelProperty(name = "created_at")
    val createdAt: String,
    val id: String,
    val message: String?,
    @JsonProperty("parent_ids")
    @ApiModelProperty(name = "parent_ids")
    val parentIds: List<String>?,
    @JsonProperty("short_id")
    @ApiModelProperty(name = "short_id")
    val shortId: String?,
    val title: String?,
    @JsonProperty("scroll_object_id")
    @ApiModelProperty(name = "scroll_object_id")
    val scrollObjectId: Any?
) {
    constructor(c: Commit) : this(
        authorEmail = c.authorEmail,
        authorName = c.authorName,
        authoredDate = c.authoredDate,
        committedDate = c.committedDate,
        committerEmail = c.committerEmail,
        committerName = c.committerName,
        createdAt = c.createdAt,
        id = c.id,
        message = c.message,
        parentIds = c.parentIds,
        shortId = c.shortId,
        title = c.title,
        scrollObjectId = c.scrollObjectId
    )
    // "
    // {
    //		"sha": "1740cfb3ba6b66d6a1bbb79d320840005135bc57",
    //		"node_id": "C_kwDOG8kG39oAKDE3NDBjZmIzYmE2YjY2ZDZhMWJiYjc5ZDMyMDg0MDAwNTEzNWJjNTc",
    //		"commit": {
    //			"author": {
    //				"name": "lockiechen",
    //				"email": "33082528+lockiechen@users.noreply.github.com",
    //				"date": "2022-06-17T10:22:51Z"
    //			},
    //			"committer": {
    //				"name": "GitHub",
    //				"email": "noreply@github.com",
    //				"date": "2022-06-17T10:22:51Z"
    //			},
    //			"message": "Merge pull request #7025 from zhanxu33/feature_6457_stream\n\nfeat: 新增Stream YAML服务 #6457",
    //			"tree": {
    //				"sha": "117caa25ab7d12c3f404728a043e4a40ae66ffa6",
    //				"url": "https://api.github.com/repos/Florence-y/bk-ci/git/trees/117caa25ab7d12c3f404728a043e4a40ae66ffa6"
    //			},
    //			"url": "https://api.github.com/repos/Florence-y/bk-ci/git/commits/1740cfb3ba6b66d6a1bbb79d320840005135bc57",
    //			"comment_count": 0,
    //			"verification": {
    //				"verified": true,
    //				"reason": "valid",
    //				"signature": "-----BEGIN PGP SIGNATURE-----\n\nwsBcBAABCAAQBQJirFX7CRBK7hj4Ov3rIwAAYEUIAG6giLG6mN381iAa6KVw1IDh\nkblB9we4fJ2Ebp7vxsqkYnNFtQDcpxfvW7YsGJjLqk6fbYzhxhx0pdjoubLJlxfy\naL1irLMo36pAqPG/VuG/6KYOTS8klwGaSLYK7DtnccsZI3cxSbMVOff/QHe0bqBX\n0nhyQepcCtEqmdNvYapI9V7nhKrJLIiX9q2M0XxmU7uH5U64MiE7Eh8IuEZF1W2s\nVuix3sR9PnE3myUj7Xa3jkaHWA0Ko27pSBp5naFNQjw8UWSHTlRS7jCknMFWbjNt\nw/wzF822FJbaag4s5GMHO/+8HMkcLneeOu5ENLMnM9SFK+ggV+Ne1Kpd60g4kb0=\n=HkWl\n-----END PGP SIGNATURE-----\n",
    //				"payload": "tree 117caa25ab7d12c3f404728a043e4a40ae66ffa6\nparent 8b0fecd53ed49f908b2f3c9b5c4be8edd1895916\nparent d10bca04bc9970de4c66f813464b225eb7d4b716\nauthor lockiechen <33082528+lockiechen@users.noreply.github.com> 1655461371 +0800\ncommitter GitHub <noreply@github.com> 1655461371 +0800\n\nMerge pull request #7025 from zhanxu33/feature_6457_stream\n\nfeat: 新增Stream YAML服务 #6457"
    //			}
    //		},
    //		"url": "https://api.github.com/repos/Florence-y/bk-ci/commits/1740cfb3ba6b66d6a1bbb79d320840005135bc57",
    //		"html_url": "https://github.com/Florence-y/bk-ci/commit/1740cfb3ba6b66d6a1bbb79d320840005135bc57",
    //		"comments_url": "https://api.github.com/repos/Florence-y/bk-ci/commits/1740cfb3ba6b66d6a1bbb79d320840005135bc57/comments",
    //		"author": {
    //			"login": "lockiechen",
    //			"id": 33082528,
    //			"node_id": "MDQ6VXNlcjMzMDgyNTI4",
    //			"avatar_url": "https://avatars.githubusercontent.com/u/33082528?v=4",
    //			"gravatar_id": "",
    //			"url": "https://api.github.com/users/lockiechen",
    //			"html_url": "https://github.com/lockiechen",
    //			"followers_url": "https://api.github.com/users/lockiechen/followers",
    //			"following_url": "https://api.github.com/users/lockiechen/following{/other_user}",
    //			"gists_url": "https://api.github.com/users/lockiechen/gists{/gist_id}",
    //			"starred_url": "https://api.github.com/users/lockiechen/starred{/owner}{/repo}",
    //			"subscriptions_url": "https://api.github.com/users/lockiechen/subscriptions",
    //			"organizations_url": "https://api.github.com/users/lockiechen/orgs",
    //			"repos_url": "https://api.github.com/users/lockiechen/repos",
    //			"events_url": "https://api.github.com/users/lockiechen/events{/privacy}",
    //			"received_events_url": "https://api.github.com/users/lockiechen/received_events",
    //			"type": "User",
    //			"site_admin": false
    //		},
    //		"committer": {
    //			"login": "web-flow",
    //			"id": 19864447,
    //			"node_id": "MDQ6VXNlcjE5ODY0NDQ3",
    //			"avatar_url": "https://avatars.githubusercontent.com/u/19864447?v=4",
    //			"gravatar_id": "",
    //			"url": "https://api.github.com/users/web-flow",
    //			"html_url": "https://github.com/web-flow",
    //			"followers_url": "https://api.github.com/users/web-flow/followers",
    //			"following_url": "https://api.github.com/users/web-flow/following{/other_user}",
    //			"gists_url": "https://api.github.com/users/web-flow/gists{/gist_id}",
    //			"starred_url": "https://api.github.com/users/web-flow/starred{/owner}{/repo}",
    //			"subscriptions_url": "https://api.github.com/users/web-flow/subscriptions",
    //			"organizations_url": "https://api.github.com/users/web-flow/orgs",
    //			"repos_url": "https://api.github.com/users/web-flow/repos",
    //			"events_url": "https://api.github.com/users/web-flow/events{/privacy}",
    //			"received_events_url": "https://api.github.com/users/web-flow/received_events",
    //			"type": "User",
    //			"site_admin": false
    //		},
    //		"parents": [
    //			{
    //				"sha": "8b0fecd53ed49f908b2f3c9b5c4be8edd1895916",
    //				"url": "https://api.github.com/repos/Florence-y/bk-ci/commits/8b0fecd53ed49f908b2f3c9b5c4be8edd1895916",
    //				"html_url": "https://github.com/Florence-y/bk-ci/commit/8b0fecd53ed49f908b2f3c9b5c4be8edd1895916"
    //			},
    //			{
    //				"sha": "d10bca04bc9970de4c66f813464b225eb7d4b716",
    //				"url": "https://api.github.com/repos/Florence-y/bk-ci/commits/d10bca04bc9970de4c66f813464b225eb7d4b716",
    //				"html_url": "https://github.com/Florence-y/bk-ci/commit/d10bca04bc9970de4c66f813464b225eb7d4b716"
    //			}
    //		]
    //	}"
    constructor(c: CommitResponse) : this(
        // todo 确定以下属性是否正确
        authorEmail = c.commit.author.email,
        authorName = c.commit.author.name,
        authoredDate = c.commit.author.date,
        committedDate = c.commit.committer.date,
        committerEmail =c.commit.committer.email,
        committerName = c.commit.committer.name,
        // todo 属性缺失
        createdAt = null,
        id = c.nodeId,
        message = c.commit.message,
        // 注意这个属性是否正确
        parentIds = c.parents.map { it.sha },
        shortId = null,
        title = null,
        scrollObjectId = null
    )
}

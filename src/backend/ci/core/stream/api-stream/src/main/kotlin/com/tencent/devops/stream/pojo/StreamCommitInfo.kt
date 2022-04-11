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
)

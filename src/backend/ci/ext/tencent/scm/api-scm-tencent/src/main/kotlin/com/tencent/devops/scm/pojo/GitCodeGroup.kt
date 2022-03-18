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

package com.tencent.devops.scm.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel

/**
{
"id": 95698,
"name": "git_01subgroup.1",
"path": "git_01subgroup.1",
"description": "",
"avatar_url": "https://git.xxx.com/assets/images/avatar/no_group_avatar.png",
"full_name": "git_01rootsubgroup/git_01subgroup.1",
"full_path": "git_01subgroup/git_01subgroup.1",
"web_url": "https://git.xxx.com/groups/git_01subgroup/git_01subgroup.1",
"parent_id": 95696
}
 */
@ApiModel("工蜂项目组列表信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitCodeGroup(
    val id: Long,
    val name: String,
    val path: String,
    val description: String?,
    @JsonProperty("avatar_url")
    val avatarUrl: String?,
    @JsonProperty("full_name")
    val fullName: String?,
    @JsonProperty("full_path")
    val fullPath: String?,
    @JsonProperty("web_url")
    val webUrl: String?,
    @JsonProperty("parent_id")
    val parentId: Long?
)

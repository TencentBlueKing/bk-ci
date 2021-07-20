/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.vo;

import com.tencent.bk.codecc.task.vo.gongfeng.ForkProjVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工蜂公用项目视图
 *
 * @version V1.0
 * @date 2019/12/4
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("工蜂公用项目视图")
public class GongfengPublicProjVO
{
    @ApiModelProperty("id")
    private Integer id;

    @ApiModelProperty("description")
    private String description;

    @ApiModelProperty("publicProj")
    private Boolean publicProj;

    @ApiModelProperty("archived")
    private Boolean archived;

    @ApiModelProperty("visibility_level")
    private Integer visibilityLevel;

//    @ApiModelProperty("namespace")
//    private NameInfoEntity nameSpace;

//    @ApiModelProperty("owner")
//    private OwnerInfoEntity owner;

    @ApiModelProperty("name")
    private String name;

    @ApiModelProperty("name_with_namespace")
    private String nameWithNameSpace;

    @ApiModelProperty("path")
    private String path;

    @ApiModelProperty("path_with_namespace")
    private String pathWithNameSpace;

    @ApiModelProperty("default_branch")
    private String defaultBranch;

    @ApiModelProperty("ssl_url_to_repo")
    private String sshUrlToRepo;

    @ApiModelProperty("http_url_to_repo")
    private String httpUrlToRepo;

    @ApiModelProperty("https_url_to_repo")
    private String httpsUrlToRepo;

    @ApiModelProperty("web_url")
    private String webUrl;

    @ApiModelProperty("tag_list")
    private List<String> tagList;

    @ApiModelProperty("issues_enabled")
    private Boolean issuesEnabled;

    @ApiModelProperty("merge_requests_enabled")
    private Boolean mergeRequestsEnabled;

    @ApiModelProperty("wiki_enabled")
    private Boolean wikiEnabled;

    @ApiModelProperty("snippets_enabled")
    private Boolean snippetsEnabled;

    @ApiModelProperty("review_enabled")
    private Boolean reviewEnabled;

    @ApiModelProperty("fork_enabled")
    private Boolean forkEnabled;

    @ApiModelProperty("tag_name_regex")
    private String tagNameRegex;

    @ApiModelProperty("tag_create_push_level")
    private Integer tagCreatePushLevel;

    @ApiModelProperty("created_at")
    private String createdAt;

    @ApiModelProperty("last_activity_at")
    private String lastActivityAt;

    @ApiModelProperty("creator_id")
    private Integer creatorId;

    @ApiModelProperty("avatar_url")
    private String avatarUrl;

    @ApiModelProperty("watchs_count")
    private Integer watchsCount;

    @ApiModelProperty("stars_count")
    private Integer starsCount;

    @ApiModelProperty("forks_count")
    private Integer forksCount;

//    @ApiModelProperty("config_storage")
//    private ConfigStorageInfoEntity configStorage;

    @ApiModelProperty("fork来源项目信息")
    private ForkProjVO forkedFromProject;

//    @ApiModelProperty("statistics")
//    private StatisticsInfoEntity statistics;
}

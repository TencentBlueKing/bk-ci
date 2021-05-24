/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 工蜂公用项目清单
 *
 * @version V1.0
 * @date 2019/9/26
 */
@Data
@Document(collection = "t_gongfeng_project")
public class GongfengPublicProjEntity extends CommonEntity
{
    @Indexed
    @Field("id")
    private Integer id;

    @Field("description")
    private String description;

    @Field("public")
    @JsonProperty("public")
    private Boolean publicProj;

    @Field("archived")
    private Boolean archived;

    @JsonProperty("visibility_level")
    @Field("visibility_level")
    private Integer visibilityLevel;

    @JsonProperty("namespace")
    @Field("namespace")
    private NameInfoEntity nameSpace;

    @Field("owner")
    private OwnerInfoEntity owner;

    @Field("name")
    private String name;

    @JsonProperty("name_with_namespace")
    @Field("name_with_namespace")
    private String nameWithNameSpace;

    @Field("path")
    private String path;

    @JsonProperty("path_with_namespace")
    @Field("path_with_namespace")
    private String pathWithNameSpace;

    @JsonProperty("default_branch")
    @Field("default_branch")
    private String defaultBranch;

    @JsonProperty("ssl_url_to_repo")
    @Field("ssl_url_to_repo")
    private String sshUrlToRepo;

    @JsonProperty("http_url_to_repo")
    @Field("http_url_to_repo")
    private String httpUrlToRepo;

    @JsonProperty("https_url_to_repo")
    @Field("https_url_to_repo")
    private String httpsUrlToRepo;

    @JsonProperty("web_url")
    @Field("web_url")
    private String webUrl;

    @JsonProperty("tag_list")
    @Field("tag_list")
    private List<String> tagList;

    @JsonProperty("issues_enabled")
    @Field("issues_enabled")
    private Boolean issuesEnabled;

    @JsonProperty("merge_requests_enabled")
    @Field("merge_requests_enabled")
    private Boolean mergeRequestsEnabled;

    @JsonProperty("wiki_enabled")
    @Field("wiki_enabled")
    private Boolean wikiEnabled;

    @JsonProperty("snippets_enabled")
    @Field("snippets_enabled")
    private Boolean snippetsEnabled;

    @JsonProperty("review_enabled")
    @Field("review_enabled")
    private Boolean reviewEnabled;

    @JsonProperty("fork_enabled")
    @Field("fork_enabled")
    private Boolean forkEnabled;

    @JsonProperty("tag_name_regex")
    @Field("tag_name_regex")
    private String tagNameRegex;

    @JsonProperty("tag_create_push_level")
    @Field("tag_create_push_level")
    private Integer tagCreatePushLevel;

    @JsonProperty("created_at")
    @Field("created_at")
    private String createdAt;

    @JsonProperty("last_activity_at")
    @Field("last_activity_at")
    private String lastActivityAt;

    @JsonProperty("creator_id")
    @Field("creator_id")
    private Integer creatorId;

    @JsonProperty("avatar_url")
    @Field("avatar_url")
    private String avatarUrl;

    @JsonProperty("watchs_count")
    @Field("watchs_count")
    private Integer watchsCount;

    @JsonProperty("stars_count")
    @Field("stars_count")
    private Integer starsCount;

    @JsonProperty("forks_count")
    @Field("forks_count")
    private Integer forksCount;

    @JsonProperty("config_storage")
    @Field("config_storage")
    private ConfigStorageInfoEntity configStorage;

    @Field("forked_from_project")
    private ForkProjEntity forkedFromProject;

    @Field("statistics")
    private StatisticsInfoEntity statistics;

    @Field("synchronize_time")
    private Long synchronizeTime;

    /**
     * 流水线id
     */
    @Field("pipeline_id")
    @Indexed(background = true)
    private String pipelineId;

    /**
     * 项目id
     */
    @Field("project_id")
    private String projectId;
}

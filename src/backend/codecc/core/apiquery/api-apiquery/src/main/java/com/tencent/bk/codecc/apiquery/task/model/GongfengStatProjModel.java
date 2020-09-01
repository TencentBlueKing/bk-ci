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

package com.tencent.bk.codecc.apiquery.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 工蜂项目度量对象实体类
 *
 * @version V1.0
 * @date 2019/12/6
 */

@Data
public class GongfengStatProjModel
{
    /**
     * 工蜂项目ID
     */
    private Integer id;

    /**
     * 事业群id
     */
    @JsonProperty("bg_id")
    private Integer bgId;

    /**
     * 项目所属的组织架构,可能有多个用逗号分隔
     */
    @JsonProperty("org_paths")
    private String orgPaths;

    /**
     * 项目路径(namespace/project)
     */
    @JsonProperty("path")
    private String path;

    /**
     * 项目描述
     */
    @JsonProperty("description")
    private String description;

    /**
     * 项目可见性
     */
    @JsonProperty("visibility")
    private String visibility;

    /**
     * 是否开源:私有(0),公共(10)
     */
    @JsonProperty("visibility_level")
    private Integer visibilityLevel;

    /**
     * 项目归属:个人(personal);团队(team)
     */
    @JsonProperty("belong")
    private String belong;

    /**
     * 项目成员user1,user2
     */
    @JsonProperty("owners")
    private String owners;

    /**
     * 创建时间
     */
    @JsonProperty("created_at")
    private String createdAt;

    /**
     * 创建者
     */
    @JsonProperty("creator")
    private String creator;

    /**
     * Web访问地址
     */
    @JsonProperty("url")
    private String url;

    /**
     * 归档状态:归档项目(true);未归档(false)
     */
    @JsonProperty("archived")
    private Boolean archived;

    /**
     * 是否为敏感项目
     */
    @JsonProperty("is_sensitive")
    private Boolean isSensitive;

    /**
     * 敏感项目的理由,如果该字段为空则表示此项目不是敏感项目
     */
    @JsonProperty("sensitive_reason")
    private String sensitiveReason;

    /**
     * 开源可见性[100:全部可见;90:不支持clone与下载;80:仅issue和wiki可见]
     */
    @JsonProperty("public_visibility")
    private Integer publicVisibility;

}

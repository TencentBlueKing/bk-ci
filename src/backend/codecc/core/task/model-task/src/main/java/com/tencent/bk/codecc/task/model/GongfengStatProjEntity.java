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
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 工蜂项目度量对象实体类
 *
 * @version V1.0
 * @date 2019/12/6
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_gongfeng_stat_project")
public class GongfengStatProjEntity extends CommonEntity
{
    /**
     * 工蜂项目ID
     */
    @Field("id")
    @Indexed
    private Integer id;

    /**
     * 事业群id
     */
    @Field("bg_id")
    @JsonProperty("bg_id")
    @Indexed
    private Integer bgId;

    /**
     * 项目所属的组织架构,可能有多个用逗号分隔
     */
    @Field("org_paths")
    @JsonProperty("org_paths")
    private String orgPaths;

    /**
     * 项目路径(namespace/project)
     */
    @Field("path")
    private String path;

    /**
     * 项目描述
     */
    @Field("description")
    private String description;

    /**
     * 项目可见性
     */
    @Field("visibility")
    private String visibility;

    /**
     * 是否开源:私有(0),公共(10)
     */
    @Field("visibility_level")
    @JsonProperty("visibility_level")
    private Integer visibilityLevel;

    /**
     * 项目归属:个人(personal);团队(team)
     */
    @Field("belong")
    private String belong;

    /**
     * 项目成员user1,user2
     */
    @Field("owners")
    private String owners;

    /**
     * 特殊情况注册了的子group用户
     */
    @Field("current_owners")
    private String currentOwners;

    /**
     * 子group用户对应的组织架构信息
     */
    @Field("current_owners_org_paths")
    private String currentOwnersOrgPaths;

    /**
     * 创建时间
     */
    @Field("created_at")
    @JsonProperty("created_at")
    private String createdAt;

    /**
     * 创建者
     */
    @Field("creator")
    private String creator;

    /**
     * Web访问地址
     */
    @Field("url")
    private String url;

    /**
     * 归档状态:归档项目(true);未归档(false)
     */
    @Field("archived")
    private Boolean archived;

    /**
     * 是否为敏感项目
     */
    @Field("is_sensitive")
    @JsonProperty("is_sensitive")
    private Boolean isSensitive;

    /**
     * 敏感项目的理由,如果该字段为空则表示此项目不是敏感项目
     */
    @Field("sensitive_reason")
    @JsonProperty("sensitive_reason")
    private String sensitiveReason;

    /**
     * 开源可见性[100:全部可见;90:不支持clone与下载;80:仅issue和wiki可见]
     */
    @Field("public_visibility")
    @JsonProperty("public_visibility")
    private Integer publicVisibility;

    /**
     * 同步时间
     */
    @Field("synchronize_time")
    @JsonProperty("synchronize_time")
    private Long synchronizeTime;

}

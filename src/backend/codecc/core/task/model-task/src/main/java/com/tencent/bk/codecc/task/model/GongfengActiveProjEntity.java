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

/**
 * 工蜂活跃项目数据实体
 * 
 * @date 2019/11/20
 * @version V1.0
 */
@Data
@Document(collection = "t_gongfeng_active_project")
public class GongfengActiveProjEntity extends CommonEntity
{
    @Field("id")
    @Indexed
    private Integer id;

    @Field("type")
    private String type;

    @Field("owners")
    private String owners;

    @Field("creator")
    private String creator;

    @JsonProperty("git_path")
    @Field("git_path")
    private String gitPath;

    @JsonProperty("hook_url")
    @Field("hook_url")
    private String hookUrl;

    @JsonProperty("created_at")
    @Field("created_at")
    private String createdAt;

    @JsonProperty("owners_org")
    @Field("owners_org")
    private String ownersOrg;

    @JsonProperty("push_count")
    @Field("push_count")
    private Integer pushCount;

    @JsonProperty("bk_project")
    @Field("bk_project")
    @Indexed
    private Boolean bkProject;

}

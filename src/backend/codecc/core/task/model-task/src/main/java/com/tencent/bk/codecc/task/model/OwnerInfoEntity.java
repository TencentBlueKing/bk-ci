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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 项目owner信息实体
 *
 * @version V1.0
 * @date 2019/9/26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OwnerInfoEntity
{
    @Field("id")
    private Integer id;

    @JsonProperty("username")
    @Field("username")
    private String userName;

    @JsonProperty("web_url")
    @Field("web_url")
    private String webUrl;
    @Field("name")
    private String name;
    @Field("state")
    private String state;

    @JsonProperty("avatar_url")
    @Field("avatar_url")
    private String avatarUrl;

}

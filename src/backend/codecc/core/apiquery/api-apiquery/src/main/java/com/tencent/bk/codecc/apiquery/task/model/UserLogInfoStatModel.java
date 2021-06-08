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
import com.tencent.bk.codecc.apiquery.defect.model.CommonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户登录统计模型
 *
 * @version V1.0
 * @date 2020/10/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserLogInfoStatModel extends CommonModel {

    @JsonProperty("user_name")
    private String userName;


    @JsonProperty("first_login")
    private Long firstLogin;


    @JsonProperty("last_login")
    private Long lastLogin;


    @JsonProperty("bg_id")
    private Integer bgId;


    @JsonProperty("dept_id")
    private Integer deptId;


    @JsonProperty("center_id")
    private Integer centerId;

}

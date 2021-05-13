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

package com.tencent.devops.common.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 用户登录统计视图
 *
 * @version V1.0
 * @date 2020/10/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("用户登录统计视图")
public class UserLogInfoStatVO extends CommonVO {

    @ApiModelProperty("用户名")
    private String userName;


    @ApiModelProperty("首次登录时间")
    private Long firstLogin;


    @ApiModelProperty("最近登录时间")
    private Long lastLogin;


    @ApiModelProperty("事业群")
    private String bgName;


    @ApiModelProperty("部门")
    private String deptName;


    @ApiModelProperty("中心")
    private String centerName;

}

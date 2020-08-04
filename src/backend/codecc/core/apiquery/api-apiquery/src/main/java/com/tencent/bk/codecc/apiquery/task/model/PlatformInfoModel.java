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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Platform信息
 *
 * @version V2.0
 * @date 2019/9/30
 */
@Data
@ApiModel("platform信息视图")
public class PlatformInfoModel
{

    @ApiModelProperty("工具名称")
    @JsonProperty("tool_name")
    private String toolName;


    @ApiModelProperty("IP")
    @JsonProperty("ip")
    private String ip;


    @ApiModelProperty("端口")
    @JsonProperty("port")
    private String port;


    @ApiModelProperty("用户名")
    @JsonProperty("user_name")
    private String userName;


    @ApiModelProperty("密码")
    @JsonProperty("passwd")
    private String passwd;


    @ApiModelProperty("token")
    @JsonProperty("token")
    private String token;


    @ApiModelProperty("状态:0-启用，1-停用")
    @JsonProperty("status")
    private Integer status;


    @ApiModelProperty("责任人")
    @JsonProperty("owner")
    private String owner;

}

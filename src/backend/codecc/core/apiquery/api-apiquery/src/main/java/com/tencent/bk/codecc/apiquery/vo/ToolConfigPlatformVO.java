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

package com.tencent.bk.codecc.apiquery.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 工具配置Platform基本信息
 *
 * @version V1.0
 * @date 2020/1/11
 */
@Data
@ApiModel("工具配置的基本信息")
public class ToolConfigPlatformVO
{
    @ApiModelProperty(value = "配置信息对应的任务ID")
    private Long taskId;

    @ApiModelProperty(value = "工具名称")
    private String toolName;

    @ApiModelProperty(value = "任务英文名")
    private String nameEn;

    @ApiModelProperty(value = "任务中文名")
    private String nameCn;

    @ApiModelProperty(value = "Platform ip")
    private String ip;

    @ApiModelProperty(value = "Platform port")
    private String port;

    @ApiModelProperty(value = "Platform 账号")
    private String userName;

    @ApiModelProperty(value = "Platform 密码")
    private String password;

    @ApiModelProperty(value = "特殊配置(用于工具侧配置文件中添加个性化属性)")
    private String specConfig;
}

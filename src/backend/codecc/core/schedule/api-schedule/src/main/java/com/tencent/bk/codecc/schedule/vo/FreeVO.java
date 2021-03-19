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
 
package com.tencent.bk.codecc.schedule.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;

/**
 * 分析任务推送请求VO
 * 
 * @date 2019/11/5
 * @version V1.0
 */
@Data
@ApiModel("分析任务推送请求VO")
@ToString
public class FreeVO
{
    @NotNull(message = "流名称不能为空")
    @ApiModelProperty(value = "流名称", required = true)
    private String streamName;

    @NotNull(message = "工具名称不能为空")
    @ApiModelProperty(value = "工具名称", required = true)
    private String toolName;

    @NotNull(message = "分析机器的IP不能为空")
    @ApiModelProperty(value = "分析机器的IP", required = true)
    private String hostIp;

    @NotNull(message = "构建ID不能为空")
    @ApiModelProperty(value = "构建ID", required = true)
    private String buildId;
}

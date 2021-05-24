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
 * 工具注册统计视图
 *
 * @version V2.0
 * @date 2020/11/23
 */
@Data
@ApiModel("工具注册统计视图")
public class ToolRegisterStatisticsVO {

    @ApiModelProperty(value = "工具中文名称")
    private String toolName;

    @ApiModelProperty(value = "工具英文名称")
    private String toolKey;

    @ApiModelProperty(value = "添加次数")
    private int addCount;

    @ApiModelProperty(value = "状态次数")
    private int registerCount;

    @ApiModelProperty(value = "未跟进次数")
    private int notFollowCount;

    @ApiModelProperty(value = "接入次数")
    private int accessCount;

    @ApiModelProperty(value = "下架次数")
    private int withdrawCount;
}

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
 * 规则告警统计视图
 */
@Data
@ApiModel("规则告警统计视图")
public class CheckerDefectStatVO {
    @ApiModelProperty("规则名称")
    private String checkerName;

    @ApiModelProperty("工具")
    private String toolName;

    @ApiModelProperty("规则创建时间")
    private Long checkerCreatedDate;

    @ApiModelProperty("开启该规则的任务数")
    private Integer openCheckerTaskCount;

    @ApiModelProperty("累计发现问题数")
    private Integer defectTotalCount;

    @ApiModelProperty("待修复问题数")
    private Integer existCount;

    @ApiModelProperty("已修复问题数")
    private Integer fixedCount;

    @ApiModelProperty("已忽略问题数")
    private Integer ignoreCount;

    @ApiModelProperty("已屏蔽问题数")
    private Integer excludedCount;

    @ApiModelProperty("统计日期")
    private Long statDate;
}

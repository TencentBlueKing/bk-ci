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

import java.util.List;

/**
 * 工具执行详情视图
 *
 * @version V1.0
 * @date 2021/2/4
 */
@Data
@ApiModel("工具执行统计视图")
public class ToolAnalyzeVO {

    @ApiModelProperty(value = "工具中文名称")
    private String toolName;

    @ApiModelProperty(value = "工具英文名称")
    private String toolKey;

    @ApiModelProperty(value = "任务ID")
    private long taskId;

    @ApiModelProperty("任务中文名")
    private String nameCn;

    @ApiModelProperty("任务英文名")
    private String nameEn;

    @ApiModelProperty("蓝盾项目ID")
    private String projectId;

    @ApiModelProperty(value = "流水线ID")
    private String pipelineId;

    @ApiModelProperty("BG")
    private String bgName;

    @ApiModelProperty("部门")
    private String deptName;

    @ApiModelProperty("接口人")
    private List<String> taskOwner;

    @ApiModelProperty(value = "执行总次数")
    private int analyzeTotalCount;

    @ApiModelProperty(value = "执行成功次数")
    private int analyzeSuccCount;

    @ApiModelProperty(value = "执行失败次数")
    private int analyzeFailCount;

    @ApiModelProperty(value = "(工具)添加时间")
    private String createTime;

    @ApiModelProperty("接入状态")
    private Integer followStatus;

    @ApiModelProperty(value = "最近一次分析状态")
    private String analyzeDate;
}

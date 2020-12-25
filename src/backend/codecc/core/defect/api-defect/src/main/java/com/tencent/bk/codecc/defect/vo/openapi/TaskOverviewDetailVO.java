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

package com.tencent.bk.codecc.defect.vo.openapi;

import com.tencent.bk.codecc.defect.vo.report.CommonChartAuthorVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 任务概览详情视图
 *
 * @version V1.0
 * @date 2020/3/16
 */

@Data
@ApiModel("任务概览详情视图")
public class TaskOverviewDetailVO
{
    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("任务英文名")
    private String nameEn;

    @ApiModelProperty("任务中文名")
    private String nameCn;

    @ApiModelProperty("任务所用语言")
    private String codeLang;

    @ApiModelProperty("任务拥有者")
    private List<String> taskOwner;

    @ApiModelProperty("蓝盾项目ID")
    private String projectId;

    @ApiModelProperty("事业群名称")
    private String bgName;

    @ApiModelProperty("部门名称")
    private String deptName;

    @ApiModelProperty("中心名称")
    private String centerName;

    @ApiModelProperty("任务状态")
    private String status;

    @ApiModelProperty("流水线ID")
    private String pipelineId;

    @ApiModelProperty("工具概览信息列表")
    private List<ToolDefectVO> toolDefectInfo;

    @Data
    public static class ToolDefectVO
    {
        @ApiModelProperty("工具名称")
        private String toolName;

        @ApiModelProperty("工具展示名称")
        private String displayName;

        @ApiModelProperty("遗留告警数")
        private Integer exist;

        @ApiModelProperty("修复告警数")
        private Integer closed;

        @ApiModelProperty("Cov遗留告警数")
        private CommonChartAuthorVO existCount;

        @ApiModelProperty("Cov修复告警数")
        private CommonChartAuthorVO closedCount;

        @ApiModelProperty("超高风险文件数量")
        private Integer superHighCount;

        @ApiModelProperty("高级别风险函数数量")
        private Integer highCount;

        @ApiModelProperty("中级别风险函数数量")
        private Integer mediumCount;

        @ApiModelProperty("低级别风险函数数量")
        private Integer lowCount;

        @ApiModelProperty("平均圈复杂度")
        private Float averageCcn;

        @ApiModelProperty("重复文件数")
        private Integer defectCount;

        @ApiModelProperty("代码重复率")
        private Float dupRate;

        @ApiModelProperty("新问题")
        private CommonChartAuthorVO newDefect;

        @ApiModelProperty("历史问题")
        private CommonChartAuthorVO historyDefect;
    }
}

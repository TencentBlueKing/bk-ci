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

package com.tencent.bk.codecc.apiquery.vo.op;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 任务多维度告警汇总视图
 *
 * @version V1.0
 * @date 2021/3/12
 */

@Data
@ApiModel("任务多维度告警汇总视图")
public class TaskDefectSummaryVO {

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

    @ApiModelProperty("最近分析状态")
    private String analyzeDate;

    @ApiModelProperty("代码库地址")
    private String repoUrl;

    @ApiModelProperty("代码库分支")
    private String branch;

    @ApiModelProperty("综合评分")
    private double rdIndicatorsScore;

    @ApiModelProperty("代码缺陷视图")
    private DimensionStatVO defectVo;

    @ApiModelProperty("安全漏洞视图")
    private DimensionStatVO securityVo;

    @ApiModelProperty("代码规范视图")
    private DimensionStatVO standardVo;

    @ApiModelProperty("圈复杂度视图")
    private DimensionStatVO ccnVo;

    @ApiModelProperty("重复率视图")
    private DimensionStatVO dupcVo;

}

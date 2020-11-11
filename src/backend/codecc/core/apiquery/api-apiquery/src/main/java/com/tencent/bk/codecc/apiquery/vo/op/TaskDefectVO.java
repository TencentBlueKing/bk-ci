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

import com.tencent.bk.codecc.apiquery.vo.report.CommonChartAuthorVO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 任务维度的告警统计视图
 *
 * @version V1.0
 * @date 2019/11/13
 */

@Data
@ApiModel("任务维度的告警统计视图")
public class TaskDefectVO {
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

    @ApiModelProperty("蓝盾项目名称")
    private String projectName;

    @ApiModelProperty("事业群名称")
    private String bgName;

    @ApiModelProperty("部门名称")
    private String deptName;

    @ApiModelProperty("中心名称")
    private String centerName;

    @ApiModelProperty("最近分析状态")
    private String analyzeDate;

    @ApiModelProperty("遗留告警超时数(OP)")
    private Integer timeoutDefectNum;

    @ApiModelProperty(value = "创建时间")
    private Long createdDate;

    @ApiModelProperty("代码行数")
    private Integer codeLineNum;

    /**----------工蜂代码库信息-----------*/
    @ApiModelProperty("代码库地址")
    private String repoUrl;

    @ApiModelProperty("代码库属于团队(team)还是个人(personal)")
    private String repoBelong;

    @ApiModelProperty("代码库是否开源:私有(0) 公共(10)")
    private Integer repoVisibilityLevel;

    @ApiModelProperty("代码库所有成员")
    private String repoOwners;

    @ApiModelProperty("fork来源工蜂项目ID(0:未fork)")
    private Integer forkedFromId;
    /**----------工蜂代码库信息-----------*/

    @ApiModelProperty("已忽略告警数")
    private Integer ignoreCount;

    @ApiModelProperty("已修复告警数")
    private CommonChartAuthorVO fixedCount;

    @ApiModelProperty("待修复告警数")
    private CommonChartAuthorVO existCount;

    @ApiModelProperty("已屏蔽告警数")
    private CommonChartAuthorVO excludedCount;

    @ApiModelProperty("新增告警数(OP)")
    private CommonChartAuthorVO newAddCount;

    public TaskDefectVO() {
//        ignoreCount = new IgnoreDefectVO();
        fixedCount = new CommonChartAuthorVO();
        existCount = new CommonChartAuthorVO();
        excludedCount = new CommonChartAuthorVO();
    }

}

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

import com.tencent.devops.common.api.CommonVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 任务信息管理扩展视图
 *
 * @version V2.0
 * @date 2020/5/11
 */
@Data
@ApiModel("任务信息管理扩展视图")
@EqualsAndHashCode(callSuper = true)
public class TaskInfoExtVO extends CommonVO
{
    @ApiModelProperty("任务ID")
    private long taskId;

    @ApiModelProperty("英文名")
    private String nameEn;

    @ApiModelProperty("中文名")
    private String nameCn;

    @ApiModelProperty("代码语言")
    private String codeLangStr;

    @ApiModelProperty("任务接口人")
    private List<String> taskOwner;

    @ApiModelProperty("任务成员")
    private List<String> taskMember;

    @ApiModelProperty("任务状态")
    private Integer status;

    @ApiModelProperty("蓝盾项目ID")
    private String projectId;

    @ApiModelProperty("蓝盾项目名")
    private String projectName;

    @ApiModelProperty("流水线ID")
    private String pipelineId;

    @ApiModelProperty("任务创建来源")
    private String createFrom;

    @ApiModelProperty("已接入的所有工具名称")
    private String toolNames;

    @ApiModelProperty("事业群")
    private String bgName;

    @ApiModelProperty("部门名称")
    private String deptName;

    @ApiModelProperty("中心名称")
    private String centerName;

    @ApiModelProperty("组名称")
    private String groupName;

    @ApiModelProperty("接入的工具列表")
    private List<ToolConfigInfoVO> toolConfigInfoList;

    @ApiModelProperty("接入的工具数")
    private Integer taskToolCount;

    @ApiModelProperty("规则数")
    private Integer checkerCount;

    @ApiModelProperty("规则集")
    private List<CheckerSetVO> checkerSetList;

    @ApiModelProperty("规则集数")
    private Integer checkerSetCount;

    @ApiModelProperty("分析次数")
    private Integer analyzeCount;

    @ApiModelProperty("跟进描述")
    private String description;

    @ApiModelProperty("最近分析状态")
    private String analyzeDate;
}

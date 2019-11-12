/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 任务详细信息
 *
 * @version V1.0
 * @date 2019/4/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("任务详细信息")
public class TaskDetailVO extends TaskBaseVO
{
    @ApiModelProperty(value = "任务成员")
    private List<String> taskMember;

    /**
     * 已接入的所有工具名称，格式; COVERITY,CPPLINT,PYLINT
     */
    @ApiModelProperty(value = "已接入的所有工具名称")
    private String toolNames;

    /**
     * 项目接入的工具列表，查询时使用
     */
    @ApiModelProperty(value = "项目接入的工具列表")
    private List<ToolConfigInfoVO> toolConfigInfoList;

    @ApiModelProperty(value = "任务停用时间")
    private String disableTime;

    @ApiModelProperty(value = "编译平台")
    private String compilePlat;

    @ApiModelProperty(value = "运行平台")
    private String runPlat;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "事业群id")
    private int bgId;

    @ApiModelProperty(value = "部门id")
    private int deptId;

    @ApiModelProperty(value = "中心id")
    private int centerId;

    @ApiModelProperty(value = "工作空间id")
    private long workspaceId;

    @ApiModelProperty(value = "凭证管理的主键id")
    private String repoHashId;

    @ApiModelProperty(value = "分支名，默认为master")
    private String branch;

    @ApiModelProperty(value = "代码库的最新版本号")
    private String repoRevision;

    @ApiModelProperty(value = "将默认过滤路径放到任务实体对象下面")
    private List<String> defaultFilterPath;

    @ApiModelProperty(value = "持续集成传递代码语言信息")
    private String devopsCodeLang;

    /**
     * 持续集成传递工具信息
     */
    @ApiModelProperty(value = "工具")
    private String devopsTools;

    @ApiModelProperty(value = "工具特定参数")
    private List<ToolConfigParamJsonVO> devopsToolParams;
}

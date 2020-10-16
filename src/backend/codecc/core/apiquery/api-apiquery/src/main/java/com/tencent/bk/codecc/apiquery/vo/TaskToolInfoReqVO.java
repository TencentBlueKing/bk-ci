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

import java.util.Collection;
import java.util.Set;

/**
 * 任务工具信息请求体
 *
 * @version V2.0
 * @date 2020/5/7
 */

@Data
@ApiModel("任务工具信息请求体")
public class TaskToolInfoReqVO
{
    @ApiModelProperty("开始时间")
    private String startTime;

    @ApiModelProperty("截止时间")
    private String endTime;

    @ApiModelProperty("任务ID集合")
    private Collection<Long> taskIds;

    @ApiModelProperty("任务中文名")
    private String nameCn;

    @ApiModelProperty("任务英文名")
    private String nameEn;

    @ApiModelProperty("蓝盾项目ID")
    private String projectId;

    @ApiModelProperty("蓝盾项目名")
    private String projectName;

    @ApiModelProperty("流水线ID")
    private String pipelineId;

    @ApiModelProperty("事业群ID")
    private Integer bgId;

    @ApiModelProperty("部门ID集合")
    private Set<Integer> deptIds;

    @ApiModelProperty("任务状态")
    private Integer status;

    @ApiModelProperty("工具名称")
    private String toolName;

    @ApiModelProperty("所用语言")
    private Long codeLang;

    @ApiModelProperty("创建来源")
    private Set<String> createFrom;

    @ApiModelProperty("是否包含新手接入 1包含,其他值不包含")
    private Integer hasNoviceRegister;

    @ApiModelProperty("跟进描述")
    private String description;

    @ApiModelProperty("告警状态: enum DefectStatus")
    private Integer defectStatus;

}

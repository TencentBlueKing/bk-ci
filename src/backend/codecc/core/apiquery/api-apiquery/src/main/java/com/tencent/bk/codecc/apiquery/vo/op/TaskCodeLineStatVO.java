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
 * task代码行数统计视图
 *
 * @version V1.0
 * @date 2021/3/2
 */

@Data
@ApiModel("task代码行数统计视图")
public class TaskCodeLineStatVO {

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("蓝盾项目ID")
    private String projectId;

    @ApiModelProperty("任务所用语言")
    private String codeLang;

    @ApiModelProperty("接入的工具")
    private String toolNames;

    @ApiModelProperty("任务拥有者")
    private List<String> taskOwner;

    @ApiModelProperty("事业群名称")
    private String bgName;

    @ApiModelProperty("部门名称")
    private String deptName;

    @ApiModelProperty("中心名称")
    private String centerName;

    @ApiModelProperty("任务状态[enum Status]")
    private Integer status;

    @ApiModelProperty("创建来源[enum BsTaskCreateFrom]")
    private String createFrom;

    @ApiModelProperty("代码仓库地址")
    private String repoUrl;

    @ApiModelProperty("代码行")
    private long codeLineCount;

    @ApiModelProperty("空行")
    private long blankCount;

    @ApiModelProperty("注释行")
    private long commentCount;

    @ApiModelProperty("总行数")
    private long totalCount;

    public Long getTotalCount() {
        return codeLineCount + blankCount + commentCount;
    }

}

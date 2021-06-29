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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 工具注册明细信息扩展视图
 */
@Data
@ApiModel("任务信息管理扩展视图")
@EqualsAndHashCode(callSuper = true)
public class ToolRegisterVO extends CommonVO {
    @ApiModelProperty("任务ID")
    private long taskId;

    @ApiModelProperty("任务英文名")
    private String nameEn;

    @ApiModelProperty("任务中文名")
    private String nameCn;

    @ApiModelProperty("BG")
    private String bgName;

    @ApiModelProperty("部门")
    private String deptName;

    @ApiModelProperty("接口人")
    private List<String> taskOwner;

    @ApiModelProperty("工具")
    private String toolName;

    @ApiModelProperty("接入状态")
    private Integer followStatus;
}

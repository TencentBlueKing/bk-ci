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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.codecc.defect.vo.LintFileVO;
import com.tencent.devops.common.api.pojo.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 指定规则包告警列表信息
 * 
 * @date 2019/11/14
 * @version V1.0
 */
@Data
@ApiModel("指定规则包告警列表信息")
public class CheckerPkgDefectVO 
{

    @ApiModelProperty("codecc代码扫描任务id")
    @JsonProperty("projId")
    private Long taskId;

    @ApiModelProperty("告警详情列表")
    private Page<DefectDetailVO> defectList;

    @ApiModelProperty("工具名")
    private String toolName;

    @ApiModelProperty("Lint类工具告警列表")
    private Page<LintFileVO> lintDefectList;
}

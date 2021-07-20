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

package com.tencent.bk.codecc.apiquery.vo.openapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规则告警统计
 *
 * @version V2.0
 * @date 2020/8/26
 */
@Data
@ApiModel("规则告警统计")
@AllArgsConstructor
@NoArgsConstructor
public class CheckerDefectStatVO {
    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("工具")
    private String toolName;

    @ApiModelProperty("规则名")
    private String checker;

    @ApiModelProperty("严重程度")
    private Integer severity;

    @ApiModelProperty("告警数")
    private Integer count;

}

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

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 工具耗时统计视图
 *
 * @version V1.0
 * @date 2021/1/15
 */

@Data
public class ToolElapseTimeVO {

    @ApiModelProperty("统计日期")
    private String date;

    @ApiModelProperty("统计数据来源: 开源/非开源(enum DefectStatType)")
    private String dataFrom;

    @ApiModelProperty("工具名")
    private String toolName;

    @ApiModelProperty("超快增量、非超快增量")
    private String scanStatType;

    @ApiModelProperty("成功分析的总耗时")
    private long totalElapseTime;

    @ApiModelProperty("分析成功次数")
    private long succAnalyzeCount;

    @ApiModelProperty("分析失败次数")
    private long failAnalyzeCount;

}

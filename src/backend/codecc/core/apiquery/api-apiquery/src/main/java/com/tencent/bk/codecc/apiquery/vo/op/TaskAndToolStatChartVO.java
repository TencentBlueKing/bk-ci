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
 * 任务和工具统计视图
 *
 * @version V1.0
 * @date 2020/12/11
 */
@Data
public class TaskAndToolStatChartVO {
    @ApiModelProperty("创建日期")
    private String date;

    @ApiModelProperty("来源")
    private String dataFrom;

    @ApiModelProperty("数量")
    private int count;

    @ApiModelProperty("活跃数量")
    private int activeCount;

    @ApiModelProperty("工具名")
    private String toolName;

    @ApiModelProperty("分析次数")
    private int analyzeCount;
}

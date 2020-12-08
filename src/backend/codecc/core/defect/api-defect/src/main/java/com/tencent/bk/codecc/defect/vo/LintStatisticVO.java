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
 
package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * lint类的统计视图
 * 
 * @date 2019/12/3
 * @version V1.0
 */
@Data
@ApiModel("lint类的统计视图")
public class LintStatisticVO extends StatisticVO
{
    @ApiModelProperty("文件数量")
    private int fileCount;

    @ApiModelProperty("文件变化数量")
    private int fileChange;

    @ApiModelProperty("新增告警数量")
    private int newDefectCount;

    @ApiModelProperty("历史告警数量")
    private int historyDefectCount;

}

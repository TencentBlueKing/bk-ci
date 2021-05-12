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

/**
 * 按工具维度统计告警视图
 *
 * @version V1.0
 * @date 2021/3/12
 */

@Data
@ApiModel("按工具维度统计告警视图")
public class DimensionStatVO {

    @ApiModelProperty("遗留告警总数")
    private int existTotalCount;

    @ApiModelProperty("遗留严重告警数")
    private int existSeriousCount;

    @ApiModelProperty("新增数")
    private int newCount;

    @ApiModelProperty("修复数")
    private int fixedCount;

    @ApiModelProperty("屏蔽数")
    private int excludedCount;

    @ApiModelProperty("工具数")
    private int toolNum;

    @ApiModelProperty("千行问题数")
    private double averageThousandDefect;

    @ApiModelProperty("平均重复率")
    private float dupRate;

    public void addExistTotalCount(Integer count) {
        if (count != null) {
            existTotalCount += count;
        }
    }

    public void addExistSeriousCount(Integer count) {
        if (count != null) {
            existSeriousCount += count;
        }
    }

    public void addNewCount(Integer count) {
        if (count != null) {
            newCount += count;
        }
    }

    public void addFixedCount(Integer count) {
        if (count != null) {
            fixedCount += count;
        }
    }

    public void addExcludedCount(Integer count) {
        if (count != null) {
            excludedCount += count;
        }
    }

    public void addToolNum() {
        toolNum++;
    }

}

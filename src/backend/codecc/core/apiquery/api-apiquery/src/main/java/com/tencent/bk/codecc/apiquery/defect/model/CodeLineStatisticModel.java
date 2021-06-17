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

package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CodeLineStatisticModel {

    /**
     * 日期
     */
    @JsonProperty("date")
    private String date;

    /**
     * 数据来源: 开源/非开源
     */
    @JsonProperty("data_from")
    private String dataFrom;

    /**
     * 代码总量
     */
    @JsonProperty("sum_code")
    private long sumCode;

    /**
     * 每日代码行
     */
    @JsonProperty("daily_code")
    private long dailyCode;

    /**
     * 每日空行
     */
    @JsonProperty("daily_blank")
    private long dailyBlank;

    /**
     * 每日注释行
     */
    @JsonProperty("daily_comment")
    private long dailyComment;

    /**
     * 获取每日总数
     * @return int
     */
    public long getDailyTotal() {
        return dailyBlank + dailyComment + dailyCode;
    }
}

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
import lombok.EqualsAndHashCode;

/**
 * 代码仓库总表配置
 *
 * @version V1.0
 * @date 2020/10/09
 */

@Data
public class CodeRepoStatisticModel {

    /**
     * 来源
     */
    @JsonProperty("data_from")
    private String dataFrom;

    /**
     * 代码仓库地址
     */

    private String url;

    /**
     * 分支名称
     */
    private String branch;

    /**
     * 代码库创建时间
     */
    @JsonProperty("url_first_scan")
    private Long urlFirstScan;

    /**
     * 分支创建日期
     */
    @JsonProperty("branch_first_scan")
    private Long branchFirstScan;

    /**
     * 分支最近修改日期
     */
    @JsonProperty("branch_last_scan")
    private Long branchLastScan;

    /**
     * 代码库数量
     */
    @JsonProperty("url_count")
    private int urlCount;

    /**
     * 分支数量
     */
    @JsonProperty("branch_count")
    private int branchCount;

}

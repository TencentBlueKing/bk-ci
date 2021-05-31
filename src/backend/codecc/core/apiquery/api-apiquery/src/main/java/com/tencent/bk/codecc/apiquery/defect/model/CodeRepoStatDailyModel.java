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

/**
 * 代码库分支每日统计Model
 *
 * @version V1.0
 * @date 2021/3/25
 */

@Data
public class CodeRepoStatDailyModel {

    /**
     * 统计日期
     */
    @JsonProperty("date")
    private String date;

    /**
     * 统计数据来源: 开源/非开源(enum DefectStatType)
     */
    @JsonProperty("data_from")
    private String dataFrom;

    /**
     * 累计代码仓库数
     */
    @JsonProperty("code_repo_count")
    private long codeRepoCount;

    /**
     * 累计分支数
     */
    @JsonProperty("branch_count")
    private long branchCount;

    /**
     * 新增代码仓库数
     */
    @JsonProperty("new_code_repo_count")
    private long newCodeRepoCount;

    /**
     * 新增分支数
     */
    @JsonProperty("new_branch_count")
    private long newBranchCount;

}

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

package com.tencent.bk.codecc.defect.model;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 代码库分支每日统计
 *
 * @version V1.0
 * @date 2021/3/18
 */

@Data
@Document(collection = "t_code_repo_stat_daily")
public class CodeRepoStatDailyEntity {

    /**
     * 统计日期
     */
    @Field
    @Indexed
    private String date;

    /**
     * 统计数据来源: 开源/非开源(enum DefectStatType)
     */
    @Field("data_from")
    @Indexed(background = true)
    private String dataFrom;

    /**
     * 累计代码仓库数
     */
    @Field("code_repo_count")
    private long codeRepoCount;

    /**
     * 累计分支数
     */
    @Field("branch_count")
    private long branchCount;

    /**
     * 新增代码仓库数
     */
    @Field("new_code_repo_count")
    private long newCodeRepoCount;

    /**
     * 新增分支数
     */
    @Field("new_branch_count")
    private long newBranchCount;

}

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
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import sun.util.resources.ga.LocaleNames_ga;

/**
 * 代码仓库 分支 实体类
 *
 * @version V1.0
 * @date 2019/6/5
 */
@Data
@Document(collection = "t_code_repo_statistic")
public class CodeRepoStatisticEntity {

    @Id
    private String entityId;

    /**
     * 来源
     */
    @Field("data_from")
    @Indexed
    private String dataFrom;

    /**
     * 代码库地址
     */
    @Field("url")
    private String url;

    /**
     * 分支名称
     */
    @Field("branch")
    private String branch;

    /**
     * 代码库创建时间
     */
    @Field("url_first_scan")
    @Indexed
    private Long urlFirstScan;

    /**
     * 分支创建时间
     */
    @Field("branch_first_scan")
    @Indexed
    private Long branchFirstScan;

    /**
     * 分支最新操作时间
     */
    @Field("branch_last_scan")
    private Long branchLastScan;
}

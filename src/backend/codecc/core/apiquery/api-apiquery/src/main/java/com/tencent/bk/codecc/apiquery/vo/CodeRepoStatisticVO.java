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

package com.tencent.bk.codecc.apiquery.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 代码库总表视图
 */

@Data
public class CodeRepoStatisticVO {
    @ApiModelProperty("代码仓库地址")
    private String url;

    @ApiModelProperty("代码库创建日期")
    private String urlFirstScan;

    @ApiModelProperty("分支名")
    private String branch;

    @ApiModelProperty("分支创建日期")
    private String branchFirstScan;

    @ApiModelProperty("分支最近修改日期")
    private String branchLastScan;

    @ApiModelProperty("数据来源")
    private String dataFrom;

    @ApiModelProperty("日期")
    private String date;

    @ApiModelProperty("代码仓库数量")
    private long urlCount;

    @ApiModelProperty("分支数")
    private long branchCount;

    @ApiModelProperty("新增代码仓库数量")
    private long newCodeRepoCount;

    @ApiModelProperty("新增分支数")
    private long newBranchCount;
}

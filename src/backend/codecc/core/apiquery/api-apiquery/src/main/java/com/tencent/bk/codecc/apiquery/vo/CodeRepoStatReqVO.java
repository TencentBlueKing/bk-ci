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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Set;

/**
 * 代码库总表信息请求体
 *
 * @version V2.0
 * @date 2020/2/25
 */
@Data
@ApiModel("代码库总表信息请求体")
public class CodeRepoStatReqVO {
    @ApiModelProperty("仓库扫描开始时间")
    private String urlStartTime;

    @ApiModelProperty("仓库扫描结束时间")
    private String urlEndTime;

    @ApiModelProperty("分支扫描开始时间")
    private String branchStartTime;

    @ApiModelProperty("分支扫描结束时间")
    private String branchEndTime;

    @ApiModelProperty("数据来源")
    private Set<String> createFrom;

    @ApiModelProperty("数据来源单选")
    private String createFromRadio;

    @ApiModelProperty("搜索框内容")
    private String searchString;
}

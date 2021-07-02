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

import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Set;

/**
 * 组织架构信息视图
 *
 * @version V2.0
 * @date 2020/02/07
 */
@Data
@ApiModel("规则集管理列表请求体")
public class CheckerSetListQueryReq {
    @ApiModelProperty("开始时间")
    private String startTime;

    @ApiModelProperty("截止时间")
    private String endTime;

    @ApiModelProperty("适用语言")
    private Set<String> codeLang;

    @ApiModelProperty("规则集类别")
    private Set<String> catagories;

    @ApiModelProperty("工具名")
    private Set<String> toolName;

    @ApiModelProperty("规则集来源")
    private Set<String> checkerSetSource;

    @ApiModelProperty("搜索框")
    private String quickSearch;
}
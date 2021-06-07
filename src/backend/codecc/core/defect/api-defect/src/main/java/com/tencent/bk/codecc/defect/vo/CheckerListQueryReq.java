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

import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerRecommendType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 查询规则清单首页实体类
 *
 * @date 2019/12/25
 * @version V1.0
 */
@Data
@ApiModel("查询规则清单首页实体类")
public class CheckerListQueryReq
{
    @ApiModelProperty("关键字")
    private String keyWord;

    @ApiModelProperty("语言")
    private Set<String> checkerLanguage;

    @ApiModelProperty("规则类型")
    private Set<CheckerCategory> checkerCategory;

    @ApiModelProperty("工具")
    private Set<String> toolName;

    @ApiModelProperty("标签")
    private Set<String> tag;

    @ApiModelProperty("严重等级")
    private Set<String> severity;

    @ApiModelProperty("可修改参数")
    private Set<Boolean> editable;

    @ApiModelProperty("推荐")
    private Set<CheckerRecommendType> checkerRecommend;

    @ApiModelProperty("规则集id")
    private String checkerSetId;

    @ApiModelProperty("版本号")
    private Integer version;

    @ApiModelProperty("是否规则集选中")
    private Set<Boolean> checkerSetSelected;



}

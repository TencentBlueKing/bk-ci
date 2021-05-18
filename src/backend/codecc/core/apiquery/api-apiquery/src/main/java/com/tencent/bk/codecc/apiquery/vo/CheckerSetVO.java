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

import com.tencent.bk.codecc.apiquery.defect.model.CheckerPropsModel;
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetCategoryModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 规则集视图
 *
 * @version V2.0
 * @date 2020/5/13
 */
@Data
@ApiModel("规则集视图")
public class CheckerSetVO
{

    @ApiModelProperty("规则集ID")
    private String checkerSetId;


    @ApiModelProperty("版本号")
    private Integer version;


    @ApiModelProperty("规则集名称")
    private String checkerSetName;


    @ApiModelProperty("规则集支持的语言")
    private Long codeLang;


    @ApiModelProperty("规则集语言文本,用于查询展示使用")
    private String checkerSetLang;


    @ApiModelProperty("规则集可见范围1：公开；2：仅我的项目")
    private Integer scope;


    @ApiModelProperty("创建者")
    private String creator;


    @ApiModelProperty("创建时间")
    private Long createTime;


    @ApiModelProperty("最近修改时间")
    private Long lastUpdateTime;


    @ApiModelProperty("规则数")
    private Integer checkerCount;


    @ApiModelProperty("规则集包含的规则和参数")
    private List<CheckerPropsModel> checkerProps;


    @ApiModelProperty("规则集被任务使用的量")
    private Integer taskUsage;


    @ApiModelProperty("是否启用1：启用；2：下架")
    private Integer enable;


    @ApiModelProperty("排序权重")
    private Integer sortWeight;


    @ApiModelProperty("项目ID")
    private String projectId;


    @ApiModelProperty("规则集描述")
    private String description;


    @ApiModelProperty("规则类型")
    private List<CheckerSetCategoryModel> catagories;


    @ApiModelProperty("基准规则集ID")
    private String baseCheckerSetId;


    @ApiModelProperty("基准规则集版本号")
    private Integer baseCheckerSetVersion;


    @ApiModelProperty("是否已初始化规则列表")
    private Boolean initCheckers;


    @ApiModelProperty("是否官方")
    private Integer official;


    @ApiModelProperty("是否是V2版本规则集，V2版本规则集只用于旧版本流水线插件")
    private Boolean legacy;


    @ApiModelProperty("来源标签")
    private String checkerSetSource;


}

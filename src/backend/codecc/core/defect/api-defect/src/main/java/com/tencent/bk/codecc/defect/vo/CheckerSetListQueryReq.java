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

import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.domain.Pageable;

import java.util.Set;

/**
 * 查询规则集清单首页实体类
 * 
 * @date 2020/1/7
 * @version V1.0
 */
@Data
@ApiModel("查询规则集清单首页实体类")
public class CheckerSetListQueryReq 
{
    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("任务id")
    private Long taskId;

    @ApiModelProperty("关键字")
    private String keyWord;

    @ApiModelProperty("语言")
    private Set<String> checkerSetLanguage;

    @ApiModelProperty("规则集类别")
    private Set<CheckerSetCategory> checkerSetCategory;

    @ApiModelProperty("工具名")
    private Set<String> toolName;

    @ApiModelProperty("规则集来源")
    private Set<CheckerSetSource> checkerSetSource;

    @ApiModelProperty("创建者")
    private String creator;

    @ApiModelProperty("快速搜索框")
    private String quickSearch;

    @ApiModelProperty("排序字段")
    private String sortField;

    @ApiModelProperty("排序字段")
    private String sortType;

    @ApiModelProperty("分页配置")
    private Integer pageNum;

    @ApiModelProperty("分页大小配置")
    private Integer pageSize;
}

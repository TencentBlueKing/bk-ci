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

package com.tencent.bk.codecc.apiquery.vo.openapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 个性化工蜂扫描任务概览视图
 *
 * @version V1.0
 * @date 2020/3/30
 */

@Data
@ApiModel("个性化工蜂扫描任务概览视图")
@EqualsAndHashCode(callSuper = true)
public class CustomTaskOverviewVO extends TaskOverviewDetailVO
{
    @ApiModelProperty("仓库地址")
    private String url;

}

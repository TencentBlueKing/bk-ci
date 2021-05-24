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

package com.tencent.bk.codecc.apiquery.vo.report;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用户登录数据图表[折线图]
 *
 * @version V1.0
 * @date 2019/10/22
 */
@Data
@ApiModel("用户登录数据的折线图表节点视图")
public class UserLogInfoChartVO {
    @ApiModelProperty("日期")
    private String date;

    @ApiModelProperty("登录用户数量")
    private Integer userLogInCount;

    @ApiModelProperty("新增用户数量")
    private Integer userAddCount;

    public UserLogInfoChartVO() {
        userLogInCount = 0;
        userAddCount = 0;
    }
}

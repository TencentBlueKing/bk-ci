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

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 规则子选项
 *
 * @version V4.0
 * @date 2019/10/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("规则子选项视图实体类")
public class CovSubcategoryVO extends CommonVO {
    @ApiModelProperty(value = "规则子选项唯一标识")
    private String checkerSubcategoryKey;

    @ApiModelProperty(value = "规则资源向名称")
    private String checkerSubcategoryName;

    @ApiModelProperty(value = "规则子选项详情")
    private String checkerSubcategoryDetail;

    @ApiModelProperty(value = "规则名唯一标识")
    private String checkerKey;

    @ApiModelProperty(value = "工具可识别规则名")
    private String checkerName;

    @ApiModelProperty(value = "语言")
    private int language;
}

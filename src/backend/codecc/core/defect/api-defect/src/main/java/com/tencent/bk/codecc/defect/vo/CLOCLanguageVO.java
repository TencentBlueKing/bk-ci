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

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * CLOC语言视图
 * 
 * @date 2020/4/9
 * @version V1.0
 */
@Data
public class CLOCLanguageVO 
{
    @ApiModelProperty("cloc语言信息")
    private String language;

    @ApiModelProperty("代码总和")
    private Long codeSum;

    @ApiModelProperty("空行总和")
    private Long blankSum;

    @ApiModelProperty("注释总和")
    private Long commentSum;

    @ApiModelProperty("有效注释总和")
    private Long efficientCommentSum;
}

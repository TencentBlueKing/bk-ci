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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Set;

/**
 * 代码库视图
 * 
 * @date 2019/12/3
 * @version V1.0
 */
@Data
@ApiModel("代码库视图")
public class CodeRepoVO 
{
    @ApiModelProperty("代码库路径")
    private String url;

    @ApiModelProperty("分支")
    private String branch;

    @ApiModelProperty("版本")
    private String version;

    @ApiModelProperty("工具名")
    private Set<String> toolNames;
}

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
 
package com.tencent.bk.codecc.defect.vo.file;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@ApiModel("文件内容")
public class ScmFileMd5Info
{
    @ApiModelProperty(value = "文件名", required = true)
    @NotNull(message = "文件名不能为空")
    private String filePath;

    @ApiModelProperty(value = "文件名", required = true)
    private String fileRelPath;

    @ApiModelProperty(value = "文件名md5", required = true)
    @NotNull(message = "文件名md5不能为空")
    private String md5;

    @ApiModelProperty(value = "创建时间", required = true)
    private long createTime;
}

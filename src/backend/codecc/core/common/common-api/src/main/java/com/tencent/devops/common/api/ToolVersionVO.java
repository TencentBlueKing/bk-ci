/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to
 * use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 工具元数据
 *
 * @version V1.0
 * @date 2019/4/24
 */
@Data
@ApiModel("工具完整信息视图")
public class ToolVersionVO {
    @ApiModelProperty("工具版本类型，T-测试版本，G-灰度版本，P-正式发布版本")
    private String versionType;

    @ApiModelProperty("docker启动运行的命令，命令由工具开发者提供，并支持带选项--json传入input.json")
    private String dockerTriggerShell;

    @ApiModelProperty("docker镜像存放URL，如：xxx.xxx.xxx.com/paas/public/tlinux2.2_codecc_tools")
    private String dockerImageURL;

    @ApiModelProperty("工具docker镜像版本号")
    private String dockerImageVersion;

    @ApiModelProperty("工具外部docker镜像版本号，用于关联第三方直接提供的docker镜像版本")
    private String foreignDockerImageVersion;

    @ApiModelProperty("docker镜像hash值")
    private String dockerImageHash;

    @ApiModelProperty("更新时间")
    private Long updatedDate;

    @ApiModelProperty("更新人")
    private String updatedBy;
}

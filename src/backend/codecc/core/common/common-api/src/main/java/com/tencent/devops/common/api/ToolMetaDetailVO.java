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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

/**
 * 工具完整信息对象
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("工具完整信息对象")
public class ToolMetaDetailVO extends ToolMetaBaseVO
{
    @ApiModelProperty(value = "工具简介，一句话介绍语", required = true)
    private String briefIntroduction;

    @ApiModelProperty(value = "工具描述，较详细的描述")
    private String description;

    @ApiModelProperty(value = "是否公开：true表示私有、false或者空表示公开")
    private boolean privated;

    @ApiModelProperty(value = "工具的图标")
    private String logo;

    @ApiModelProperty(value = "工具的图文详情")
    private String graphicDetails;

    @ApiModelProperty(value = "工具支持的代码语言列表", required = true)
    private List<String> supportedLanguages;

    @ApiModelProperty(value = "docker启动运行的命令，命令由工具开发者提供，并支持带选项--json传入input.json", required = true)
    private String dockerTriggerShell;

    @ApiModelProperty(value = "docker镜像存放URL，如：xxx.xxx.xxx.com/paas/public/tlinux2.2_codecc_tools", required = true)
    private String dockerImageURL;

    @ApiModelProperty(value = "docker镜像版本", required = true)
    private String dockerImageVersion;

    @ApiModelProperty(value = "docker镜像版本类型", required = true)
    private String dockerImageVersionType;

    @ApiModelProperty(value = "工具外部docker镜像版本号，用于关联第三方直接提供的docker镜像版本")
    private String foreignDockerImageVersion;

    @ApiModelProperty(value = "docker镜像仓库账号")
    private String dockerImageAccount;

    @ApiModelProperty(value = "docker镜像仓库密码")
    private String dockerImagePasswd;

    @ApiModelProperty(value = "调试流水线Id", required = true)
    private String debugPipelineId;

    @ApiModelProperty(value = "工具bin目录路径")
    private String toolHomeBin;

    @ApiModelProperty(value = "工具扫描命令")
    private String toolScanCommand;

    @ApiModelProperty(value = "工具环境")
    private String toolEnv;

    @ApiModelProperty(value = "工具运行类型：docker,local")
    private String toolRunType;

    @ApiModelProperty(value = "工具历史版本号列表")
    private List<String> toolHistoryVersion;

    @ApiModelProperty(value = "工具支持的参数列表")
    private List<ToolOption> toolOptions;

    @ApiModelProperty(value = "用户自定义关注的工具信息")
    private CustomToolInfo customToolInfo;
    
    @Data
    public static class ToolOption
    {
        @ApiModelProperty(value = "参数名", required = true)
        private String varName;

        @ApiModelProperty(value = "参数类型，可选值：NUMBER,STRING,BOOLEAN,RADIO,CHECKBOX，分别表示：数字，字符串，布尔值，单选框，复选框", required = true)
        private String varType;

        @ApiModelProperty(value = "参数展示名，不填展示varName")
        private String labelName;

        @ApiModelProperty(value = "参数默认值")
        private String varDefault;

        @ApiModelProperty(value = "参数说明")
        private String varTips;

        @ApiModelProperty(value = "参数是否必填：true必填，false非必填")
        private boolean varRequired;

        @ApiModelProperty(value = "varType为RADIO或CHECKBOX时必填，表示单选框或复选框的选项列表")
        private List<VarOption> varOptionList;
    }

    @Data
    public static class VarOption
    {
        private String name;
        private String id;
    }

    @Data
    public static class CustomToolInfo {
        @ApiModelProperty(value = "工具上报关注参数")
        private Map<String, String> customToolParam;

        @ApiModelProperty(value = "上报告警统计维度")
        private Map<String, String> customToolDimension;
    }
}

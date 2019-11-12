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

package com.tencent.bk.codecc.task.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 告警配置详情视图
 *
 * @version V1.0
 * @date 2019/5/20
 */
@Data
@ApiModel("告警配置详情视图")
public class DefectConfigInfoVO
{

    @ApiModelProperty("任务id")
    @JsonProperty("task_id")
    private long taskId;


    @ApiModelProperty("任务英文名")
    @JsonProperty("stream_name")
    private String nameEn;


    @ApiModelProperty("代码库路径")
    private String url;


    @ApiModelProperty("代码库密码")
    @JsonProperty("password")
    private String passWord;


    @ApiModelProperty("多工具类型")
    @JsonProperty("multi_tool_type")
    private String multiToolType;


    @ApiModelProperty("过滤路径")
    @JsonProperty("skip_paths")
    private String skipPaths;

    @ApiModelProperty("屏蔽规则")
    @JsonProperty("skip_checkers")
    private String skipCheckers;

    @ApiModelProperty("账号")
    private String account;


    @ApiModelProperty(value = "代码库类型", allowableValues = "{svn,git}")
    @JsonProperty("scm_type")
    private String scmType;

    @ApiModelProperty(value = "鉴权方式", allowableValues = "{http,ssh}")
    @JsonProperty("cert_type")
    private String certType;

    @ApiModelProperty("git分支")
    @JsonProperty("git_branch")
    private String gitBranch;

    @ApiModelProperty("oAuth的access token")
    @JsonProperty("access_token")
    private String accessToken;

    @ApiModelProperty("ssh鉴权方式的私钥")
    @JsonProperty("ssh_private_key")
    private String sshPrivateKey;

    @ApiModelProperty(value = "扫描类型0:全量扫描,1:增量扫描", allowableValues = "{0,1}")
    @JsonProperty("scan_type")
    private String scanType;

    @ApiModelProperty(value = "项目接口人")
    @JsonProperty("proj_owner")
    private String projOwner;

    @ApiModelProperty(value = "工具打开的规则")
    @JsonProperty("open_checkers")
    private String openCheckers;

    @ApiModelProperty(value = "工具打开的规则")
    @JsonProperty("checker_options")
    private String checkerOptions;

    @ApiModelProperty(value = "工具特殊参数")
    @JsonProperty("param_json")
    private String paramJson;


}

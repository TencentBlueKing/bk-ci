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

package com.tencent.bk.codecc.defect.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 告警上报入参的抽象类
 *
 * @version V1.0
 * @date 2019/5/15
 */
@Data
@ApiModel("告警上报入参的抽象类")
public class UploadDefectVO
{
    @ApiModelProperty(value = "流名称", required = true)
    @JsonProperty("stream_name")
    private String streamName;

    @ApiModelProperty("任务id")
    @JsonProperty("task_id")
    private Long taskId;

    @ApiModelProperty(value = "工具名称", required = true)
    @JsonProperty("tool_name")
    private String toolName;

    @ApiModelProperty(value = "文件路径(代码下载到服务器上的存放路径)", required = true)
    @JsonProperty("filename")
    private String filePath;

    @ApiModelProperty(value = "文件最近修改时间", required = true)
    @JsonProperty("file_change_time")
    private long fileUpdateTime;

    @ApiModelProperty(value = "告警压缩后的字符串", required = true)
    private String defectsCompress;

    @ApiModelProperty(value = "构建ID")
    private String buildId;

    @ApiModelProperty("代码库路径")
    private String url;

    @ApiModelProperty("代码仓库id")
    @JsonProperty("repo_id")
    private String repoId;

    @ApiModelProperty("版本号")
    private String revision;

    @ApiModelProperty("分支名称")
    private String branch;

    @ApiModelProperty(value = "相对路径", required = true)
    @JsonProperty("rel_path")
    private String relPath;

    @ApiModelProperty("代码库子模块")
    @JsonProperty("sub_module")
    private String subModule;

    @ApiModelProperty("cloc工具信息")
    @JsonProperty("cloc_info")
    private CLOCInfoVO clocInfo;

    @ApiModelProperty("第三方平台的项目ID")
    @JsonProperty("projectId")
    private String platformProjectId;

    @ApiModelProperty("第三方平台的报告ID")
    private String reportId;

    @ApiModelProperty("重复行数")
    @JsonProperty("dup_line_count")
    private Long dupcLineCount;

    @ApiModelProperty("总行数")
    @JsonProperty("total_line_count")
    private Long totalLineCount;
}

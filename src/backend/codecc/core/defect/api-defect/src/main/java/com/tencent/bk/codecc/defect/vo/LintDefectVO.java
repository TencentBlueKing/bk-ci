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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * lint类告警视图
 *
 * @version V1.0
 * @date 2019/5/9
 */
@Data
@ApiModel("lint类告警视图")
public class LintDefectVO
{
    @ApiModelProperty(value = "所属文件的主键id")
    private String entityId;

    @ApiModelProperty("告警ID")
    private String id;

    @ApiModelProperty(value = "所属文件名")
    private String fileName;

    @ApiModelProperty("告警行号")
    private int lineNum;

    @ApiModelProperty("告警作者")
    private String author;

    @ApiModelProperty("告警规则")
    private String checker;

    @ApiModelProperty("告警规则名")
    private String checkerName;

    @ApiModelProperty("严重程度")
    private int severity;

    @ApiModelProperty("告警描述")
    private String message;

    /**
     * 告警类型：新告警NEW(1)，历史告警HISTORY(2)
     */
    @ApiModelProperty("告警类型")
    private int defectType;

    /**
     * 告警状态：NEW(1), FIXED(2), IGNORE(4), PATH_MASK(8), CHECKER_MASK(16);
     */
    @ApiModelProperty("告警状态")
    private int status;

    /**
     * 告警行的变更时间，用于跟新旧告警的判断时间做对比
     */
    @ApiModelProperty("告警行的变更时间")
    private long lineUpdateTime;

    @ApiModelProperty("pinpoint的hash值")
    private String pinpointHash;

    @ApiModelProperty("文件的md5值")
    private String fileMd5;

    @ApiModelProperty("文件相对路径")
    private String relPath;

    @ApiModelProperty("文件全路径")
    private String filePath;

    @ApiModelProperty("告警规则详情")
    private String checkerDetail;

    @ApiModelProperty("告警创建时间")
    private Long createTime;

    @ApiModelProperty("告警修复时间")
    private Long fixedTime;

    @ApiModelProperty("告警忽略时间")
    private Long ignoreTime;

    @ApiModelProperty("告警忽略原因类型")
    private Integer ignoreReasonType;

    @ApiModelProperty("告警忽略原因")
    private String ignoreReason;

    @ApiModelProperty("告警忽略操作人")
    private String ignoreAuthor;

    @ApiModelProperty("告警屏蔽时间")
    private Long excludeTime;

    /**
     * 告警是否被标记为已修改的标志，checkbox for developer, 0 is normal, 1 is tag, 2 is prompt
     */
    @ApiModelProperty(value = "告警是否被标记为已修改的标志")
    private Integer mark;

    @ApiModelProperty(value = "告警被标记为已修改的时间")
    private Long markTime;

    @ApiModelProperty("创建时的构建号")
    private String createBuildNumber;

    @ApiModelProperty("修复时的构建号")
    private String fixedBuildNumber;

    @ApiModelProperty("告警规则类型")
    private String checkerType;

    @ApiModelProperty("告警详情链接")
    private String defectDetailUrl;

    @ApiModelProperty("告警评论")
    private CodeCommentVO codeComment;

    @ApiModelProperty("版本号")
    private String revision;

    @ApiModelProperty("工具名")
    private String toolName;
}

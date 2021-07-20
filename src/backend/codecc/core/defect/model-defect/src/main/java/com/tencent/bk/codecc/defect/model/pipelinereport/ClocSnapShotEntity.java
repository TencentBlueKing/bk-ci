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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.model.pipelinereport;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 工具快照实体类
 *
 * @version V1.0
 * @date 2019/6/27
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClocSnapShotEntity extends ToolSnapShotEntity {
    /**
     * 分析开始时间
     */
    @Field("start_time")
    @JsonProperty("start_time")
    private long startTime;

    /**
     * 分析结束时间
     */
    @Field("end_time")
    @JsonProperty("end_time")
    private long endTime;

    /**
     * 所有代码总行数
     */
    @Field("total_lines")
    @JsonProperty("total_lines")
    private long totalLines;

    /**
     * 相比上一次构建，总代码行数变化
     */
    @Field("total_change")
    @JsonProperty("total_change")
    private Long totalChange;

    /**
     * 空白行总数
     */
    @Field("total_blank")
    @JsonProperty("total_blank")
    private Long totalBlank;

    /**
     * 代码行总数
     */
    @Field("total_code")
    @JsonProperty("total_code")
    private Long totalCode;

    /**
     * 注释行总数
     */
    @Field("total_comment")
    @JsonProperty("total_comment")
    private Long totalComment;

    /**
     * 相较于上一次构建，空白行总数变化
     */
    @Field("total_blank_change")
    @JsonProperty("total_blank_change")
    private Long totalBlankChange;

    /**
     * 相较于上一次构建，代码行总数变化
     */
    @Field("total_code_change")
    @JsonProperty("total_code_change")
    private Long totalCodeChange;

    /**
     * 相较于上一次构建，注释行总数变化
     */
    @Field("total_comment_change")
    @JsonProperty("total_comment_change")
    private Long totalCommentChange;
}
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
import com.tencent.bk.codecc.defect.vo.common.ToolSnapShotVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * lint类工具快照视图
 *
 * @version V1.0
 * @date 2019/6/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("lint类工具快照视图")
public class LintSnapShotVO extends ToolSnapShotVO
{
    @ApiModelProperty("最近一次分析，接入后新增文件总的缺陷告警个数")
    @JsonProperty("newfile_total_defect_count")
    private int newFileTotalDefectCount;

    @ApiModelProperty("最近一次分析，接入后新增文件变化的缺陷个数")
    @JsonProperty("newfile_changed_defect_count")
    private int newFileChangedDefectCount;

    @ApiModelProperty("最近一次分析，接入后新增文件总个数")
    @JsonProperty("newfile_total_count")
    private int newFileTotalCount;

    @ApiModelProperty("最近一次分析，接入后新增文件变化的个数")
    @JsonProperty("newfile_changed_count")
    private int newFileChangedCount;

    @ApiModelProperty("严重")
    @JsonProperty("total_new_serious")
    private int totalNewSerious;

    @ApiModelProperty("一般")
    @JsonProperty("total_new_normal")
    private int totalNewNormal;

    @ApiModelProperty("提示")
    @JsonProperty("total_new_prompt")
    private int totalNewPrompt;

    @ApiModelProperty("未修复告警作者清单")
    @JsonProperty("author_list")
    private List<NotRepairedAuthorVO> authorList;

}

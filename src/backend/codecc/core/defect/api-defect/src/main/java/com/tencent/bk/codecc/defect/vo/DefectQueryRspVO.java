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

import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Page;

import java.util.Map;

/**
 * 圈复杂度查询返回视图
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("圈复杂度查询返回视图")
public class DefectQueryRspVO extends CommonDefectQueryRspVO
{
    @ApiModelProperty("严重告警数")
    private int seriousCount;

    @ApiModelProperty("一般告警数")
    private int normalCount;

    @ApiModelProperty("提示告警数")
    private int promptCount;

    @ApiModelProperty("待修复告警数")
    private int existCount;

    @ApiModelProperty("已修复告警数")
    private int fixCount;

    @ApiModelProperty("已忽略告警数")
    private int ignoreCount;

    @ApiModelProperty("已屏蔽告警数")
    private int maskCount;

    @ApiModelProperty("新告警数")
    private int newCount;

    @ApiModelProperty("历史告警数")
    private int historyCount;

    @ApiModelProperty("总告警数")
    private int totalCount;

    @ApiModelProperty("新老告警判定时间")
    private long newDefectJudgeTime;

    @ApiModelProperty("文件路径树")
    private TreeNodeVO filePathTree;

    @ApiModelProperty("告警列表")
    private Map<String, Integer> checkerMap;

    @ApiModelProperty("作者列表")
    private Map<String, Integer> authorMap;

    @ApiModelProperty("告警清单")
    private Page<DefectBaseVO> defectList;
}

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

import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 圈复杂度告警查询返回视图
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("圈复杂度告警查询返回视图")
public class CCNDefectDetailQueryRspVO extends CommonDefectDetailQueryRspVO
{
    @ApiModelProperty("代码评论")
    private CodeCommentVO codeComment;   // todo delete

    /**
     * 告警忽略时间
     */
    @ApiModelProperty(value = "告警忽略时间")
    private Long ignoreTime;

    /**
     * 告警忽略原因类型
     */
    @ApiModelProperty("告警忽略原因类型")
    private Integer ignoreReasonType;

    /**
     * 告警忽略原因
     */
    @ApiModelProperty("告警忽略原因")
    private String ignoreReason;

    /**
     * 告警忽略操作人
     */
    @ApiModelProperty("告警忽略操作人")
    private String ignoreAuthor;

    @ApiModelProperty("告警详细信息")
    private CCNDefectVO defectVO;
}

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

/**
 * 重复率的告警（文件）列表查询响应体
 *
 * @version V1.0
 * @date 2019/6/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("重复率列表查询返回视图")
public class DUPCDefectQueryRspVO extends CommonDefectQueryRspVO
{
    /**
     * 风险系数极高的个数
     */
    @ApiModelProperty("风险系数极高的个数")
    private int superHighCount;

    /**
     * 风险系数高的个数
     */
    @ApiModelProperty("风险系数高的个数")
    private int highCount;

    /**
     * 风险系数中的个数
     */
    @ApiModelProperty("风险系数中的个数")
    private int mediumCount;

    /**
     * 告警总数
     */
    private int totalCount;

    /**
     * 缺陷列表
     */
    private Page<DUPCDefectVO> defectList;

    /**
     * 新老告警判定时间
     */
    @ApiModelProperty("新老告警判定时间")
    private long newDefectJudgeTime;

    /**
     * 新增告警的个数
     */
    @ApiModelProperty("新增告警的个数")
    private int newCount;

    /**
     * 历史告警的个数
     */
    @ApiModelProperty("历史告警的个数")
    private int historyCount;
}

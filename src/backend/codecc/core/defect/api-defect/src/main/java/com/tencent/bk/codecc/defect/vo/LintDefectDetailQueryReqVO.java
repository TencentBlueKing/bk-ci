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

import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;

/**
 * lint类工具查询请求视图
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("lint类工具查询请求视图")
public class LintDefectDetailQueryReqVO extends CommonDefectDetailQueryReqVO
{
    @ApiModelProperty("告警id")
    private String defectId;

    @ApiModelProperty("规则包id")
    private String pkgId;

    @ApiModelProperty("告警规则")
    private String checker;

    @ApiModelProperty("处理人")
    private String author;

    @ApiModelProperty("严重程度")
    private Set<String> severity;

    @ApiModelProperty(value = "告警状态：待修复（1），已修复（2），忽略（4），路径屏蔽（8），规则屏蔽（16）", allowableValues = "{1,2,4,8,16}")
    private Set<String> status;

    @ApiModelProperty(value = "文件或路径列表")
    private Set<String> fileList;

    @ApiModelProperty(value = "聚类类型:文件(file),问题(defect)", allowableValues = "{file,defect}")
    private String clusterType;

    @ApiModelProperty(value = "起始创建时间")
    private String startCreateTime;

    @ApiModelProperty(value = "截止创建时间")
    private String endCreateTime;

    @ApiModelProperty(value = "告警类型:新增(1),历史(2)", allowableValues = "{1,2}")
    private Set<String> defectType;

    @ApiModelProperty(value = "构建ID")
    private String buildId;
}

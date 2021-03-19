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

import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * lint类配置规则包视图
 *
 * @version V1.0
 * @date 2019/6/5
 */
@Data
@ApiModel("lint类配置规则包视图")
public class CheckerPkgRspVO
{

    @ApiModelProperty("规则包ID")
    private String pkgId;

    @ApiModelProperty("规则包名称")
    private String pkgName;

    @ApiModelProperty("规则包描述")
    private String pkgDesc;

    @ApiModelProperty("规则包状态是否打开")
    private Boolean pkgStatus;

    @ApiModelProperty("规则包打开的个数")
    private Integer openCheckerNum;

    @ApiModelProperty("总的规则个数")
    private Integer totalCheckerNum;

    @ApiModelProperty("规则列表")
    private List<CheckerDetailVO> checkerList;

    @ApiModelProperty("规则集")
    private CheckerSetVO checkerSet;
}

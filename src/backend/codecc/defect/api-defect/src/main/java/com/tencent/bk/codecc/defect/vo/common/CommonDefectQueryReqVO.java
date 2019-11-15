/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.defect.vo.common;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tencent.bk.codecc.defect.vo.LintDefectQueryReqVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;


/**
 * 公共告警详情查询请求视图
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Data
@ApiModel("公共告警详情查询请求视图")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "pattern", visible = true, defaultImpl = CommonDefectQueryReqVO.class)
@JsonSubTypes({@JsonSubTypes.Type(value = LintDefectQueryReqVO.class, name = "LINT")
})
public class CommonDefectQueryReqVO
{
    @ApiModelProperty(value = "LINT告警主键id", required = true)
    @NotNull(message = "LINT告警主键id不能为空")
    private String entityId;

    @ApiModelProperty(value = "工具名", required = true)
    @NotNull(message = "工具名不能为空")
    private String toolName;

    @ApiModelProperty(value = "工具模型", required = true)
    @NotNull(message = "工具模型不能为空")
    private String pattern;

    @ApiModelProperty(value = "文件路径", required = true)
    @NotNull(message = "文件路径不能为空")
    private String filePath;
}

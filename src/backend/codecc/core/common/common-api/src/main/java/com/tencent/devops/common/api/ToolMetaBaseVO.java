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

package com.tencent.devops.common.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * 工具完整信息对象
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Data
@ApiModel("工具完整信息视图")
public class ToolMetaBaseVO extends CommonVO
{
    /**
     * 工具模型,LINT、COMPILE、TSCLUA、CCN、DUPC，决定了工具的接入、告警、报表的处理及展示类型
     */
    @ApiModelProperty("工具模型,决定了工具的接入、告警、报表的处理及展示类型")
    private String pattern;

    /**
     * 工具名称，也是唯一KEY
     */
    @ApiModelProperty(value = "工具名称，也是唯一KEY", required = true)
    @Pattern(regexp = "[A-Z]+", message = "工具名称，只能包含大写字母")
    private String name;

    /**
     * 工具的展示名
     */
    @ApiModelProperty(value = "工具的展示名", required = true)
    private String displayName;

    /**
     * 工具类型，界面上展示工具归类：
     * 发现缺陷和安全漏洞、规范代码、复杂度、重复代码
     */
    @ApiModelProperty(value = "工具类型", required = true)
    private String type;

    /**
     * 支持语言，通过位运算的值表示
     */
    @ApiModelProperty("支持语言，通过位运算的值表示")
    private long lang;

    /**
     * 根据项目语言来判断是否推荐该款工具,true表示推荐，false表示不推荐
     */
    @ApiModelProperty("根据项目语言来判断是否推荐该款工具,true表示推荐，false表示不推荐")
    private boolean recommend;

    /**
     * 状态：测试（T）、灰度（保留字段）、发布（P）、下架， 注：测试类工具只有管理员可以在页面上看到，只有管理员可以接入
     */
    @ApiModelProperty(value = "态：测试（T）、灰度（保留字段）、发布（P）、下架， 注：测试类工具只有管理员可以在页面上看到，只有管理员可以接入", allowableValues = "{T,P}")
    private String status;

    /**
     * 工具的个性参数，如pylint的Python版本，这个参数用json保存。
     * 用户在界面上新增参数，填写参数名，参数变量， 类型（单选、复选、下拉框等），枚举值
     */
    @ApiModelProperty("工具的个性参数")
    private String params;

    /**
     * 工具版本号
     */
    @ApiModelProperty("工具版本")
    private String toolVersion;

    /**
     * 最新的工具镜像版本（hash值）
     */
    @ApiModelProperty("工具镜像版本")
    private String toolImageRevision;

    /**
     * 工具版本列表，T-测试版本，G-灰度版本，P-正式发布版本
     */
    @ApiModelProperty("工具镜像版本")
    private List<ToolVersionVO> toolVersions;
}

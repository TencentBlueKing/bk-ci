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

package com.tencent.bk.codecc.apiquery.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.codecc.apiquery.defect.model.CommonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具元数据
 *
 * @version V1.0
 * @date 2019/4/24
 */
@Data
public class ToolMetaModel
{
    /**
     * 工具名称，也是唯一KEY
     */
    @JsonProperty("name")
    private String name;

    /**
     * 工具模型,LINT、COVERITY、KLOCWORK、TSCLUA、CCN、DUPC，决定了工具的接入、告警、报表的处理及展示类型
     */
    @JsonProperty("pattern")
    private String pattern;

    /**
     * 工具的展示名
     */
    @JsonProperty("display_name")
    private String displayName;

    /**
     * 工具类型，界面上展示工具归类：
     * 发现缺陷和安全漏洞、规范代码、复杂度、重复代码
     */
    @JsonProperty("type")
    private String type;

    /**
     * 支持语言，通过位运算的值表示
     */
    @JsonProperty("lang")
    private long lang;

    /**
     * 工具简介，一句话介绍语
     */
    @JsonProperty("brief_introduction")
    private String briefIntroduction;

    /**
     * 工具描述，较详细的描述
     */
    @JsonProperty("description")
    private String description;

    /**
     * 工具的个性参数，如pylint的Python版本，这个参数用json保存。
     * 用户在界面上新增参数，填写参数名，参数变量， 类型（单选、复选、下拉框等），枚举值
     */
    @JsonProperty("params")
    private String params;

    /**
     * 工具的图标
     */
    @JsonProperty("logo")
    private String logo;

    /**
     * 工具的图文详情
     */
    @JsonProperty("graphic_details")
    private String graphicDetails;

    /**
     * 状态：测试（T）、灰度（保留字段）、发布（P）、下架， 注：测试类工具只有管理员可以在页面上看到，只有管理员可以接入
     */
    @JsonProperty("status")
    private String status;

    /**
     * 是否公开：true表示私有、false或者空表示公开
     */
    @JsonProperty("privated")
    private boolean privated;

    /**
     * 是否同步上蓝盾：true表示同步、false或者空表示不同步
     */
    @JsonProperty("sync_ld")
    private boolean syncLD;
}

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

package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 规则详细信息实体类
 *
 * @version V1.0
 * @date 2019/4/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_checker_detail")
public class CheckerDetailEntity extends CommonEntity
{
    /**
     * 工具名
     */
    @Field("tool_name")
    @Indexed
    private String toolName;

    /**
     * 告警类型key，唯一标识，如：qoc_lua_UseVarIfNil
     */
    @Field("checker_key")
    private String checkerKey;

    /**
     * 规则名称
     */
    @Field("checker_name")
    private String checkerName;

    /**
     * 规则详细描述
     */
    @Field("checker_desc")
    private String checkerDesc;

    /**
     * 规则严重程度，1=>严重，2=>一般，3=>提示
     */
    private int severity;

    /**
     * 规则所属语言（针对KLOCKWORK）
     */
    private int language;

    /**
     * 规则状态 2=>打开 1=>关闭;
     */
    private int status;

    /**
     * 规则类型
     */
    @Field("checker_type")
    private String checkerType;

    /**
     * 规则类型说明
     */
    @Field("checker_type_desc")
    private String checkerTypeDesc;

    /**
     * 规则类型排序序列号
     */
    @Field("checker_type_sort")
    private String checkerTypeSort;

    /**
     * 所属规则包
     */
    @Field("pkg_kind")
    private String pkgKind;

    /**
     * 项目框架（针对Eslint工具,目前有vue,react,standard）
     */
    @Field("framework_type")
    private String frameworkType;

    @Field("checker_mapped")
    private String checkerMapped;

    /**
     * 规则配置
     */
    private String props;

    /**
     * 规则所属标准
     */
    private int standard;

    /**
     * 规则是否支持配置true：支持;空或false：不支持
     */
    private String editable;

    /**
     * 示例代码
     */
    @Field("code_example")
    private String codeExample;


}

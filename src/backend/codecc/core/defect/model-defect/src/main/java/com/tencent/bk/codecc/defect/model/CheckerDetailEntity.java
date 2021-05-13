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
import com.tencent.devops.common.constant.ComConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Set;

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
     * 规则详细描述-带占位符
     */
    @Field("checker_desc_model")
    private String checkerDescModel;

    /**
     * 规则严重程度，1=>严重，2=>一般，3=>提示
     */
    @Indexed
    private int severity;

    /**
     * 规则所属语言（针对KLOCKWORK）
     */
    @Field("language")
    private long language;

    /**
     * 规则状态 0=>打开 1=>关闭;
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
    @Indexed
    private Boolean editable;

    /**
     * 示例代码
     */
    @Field("code_example")
    private String codeExample;

    /**
     * 规则子类
     */
    @Field("cov_issue_type")
    private String covIssueType;

    /**
     * 规则子类
     */
    @Field("cov_property")
    private int covProperty;

    /**
     * 是否原生规则
     */
    @Field("native_checker")
    private Boolean nativeChecker;


    /**
     * 是否原生规则
     */
    @Field("cov_subcategory")
    private List<CovSubcategoryEntity> covSubcategory;

    @Transient
    private Boolean checkerSetSelected;


    /*-------------------根据改动新增规则字段---------------------*/
    /**
     * 规则对应语言，都存文字，mongodb对按位与不支持
     */
    @Field("checker_language")
    @Indexed
    private Set<String> checkerLanguage;

    /**
     * 规则类型
     */
    @Field("checker_category")
    @Indexed
    private String checkerCategory;

    /**
     * 规则标签
     */
    @Field("checker_tag")
    @Indexed
    private Set<String> checkerTag;

    /**
     * 规则推荐类型
     */
    @Field("checker_recommend")
    @Indexed
    private String checkerRecommend;

    @Field("err_example")
    private String errExample;

    @Field("right_example")
    private String rightExample;

    /**
     * 规则版本：T-测试/G-灰度/P-正式
     * 测试版本规则只能在测试项目看到和选择到，灰度版本规则只能在灰度项目看到和选择到。
     * 包含了测试规则的规则集就是测试版本的规则集，只能在测试项目被选择到；
     * 包含了灰度规则的规则集就是灰度版本的规则集，只能在灰度项目被选择到。
     */
    @Field("checker_version")
    private int checkerVersion;
}

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

package com.tencent.bk.codecc.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 基础数据表，要保持这个表只有少量的数据
 *
 * @version V1.0
 * @date 2019/4/24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_base_data")
public class BaseDataEntity extends CommonEntity
{
    @Indexed
    @Field("param_code")
    private String paramCode;

    @Field("param_name")
    private String paramName;

    @Field("param_value")
    private String paramValue;

    @Field("param_type")
    private String paramType;

    @Field("param_status")
    private String paramStatus;

    @Field("param_extend1")
    private String paramExtend1;

    @Field("param_extend2")
    private String paramExtend2;

    @Field("param_extend3")
    private String paramExtend3;

    @Field("param_extend4")
    private String paramExtend4;

    @Field("param_extend5")
    private String paramExtend5;

    @Field("lang_full_key")
    private String langFullKey;

    @Field("lang_type")
    private String langType;

    @Field("open_source_checker_sets")
    private List<OpenSourceCheckerSet> openSourceCheckerSets;

    @Field("epc_checker_sets")
    private List<OpenSourceCheckerSet> epcCheckerSets;

    @Field("cloc_lang")
    private String clocLang;
}

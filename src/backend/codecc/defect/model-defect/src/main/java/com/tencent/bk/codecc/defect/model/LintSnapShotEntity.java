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
 
package com.tencent.bk.codecc.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.codecc.defect.model.common.ToolSnapShotEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * lint类工具快照实体类
 * 
 * @date 2019/6/28
 * @version V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LintSnapShotEntity extends ToolSnapShotEntity
{

    @Field("newfile_total_defect_count")
    @JsonProperty("newfile_total_defect_count")
    private int newFileTotalDefectCount;

    @Field("newfile_changed_defect_count")
    @JsonProperty("newfile_changed_defect_count")
    private int newFileChangedDefectCount;

    @Field("newfile_total_count")
    @JsonProperty("newfile_total_count")
    private int newFileTotalCount;

    @Field("newfile_changed_count")
    @JsonProperty("newfile_changed_count")
    private int newFileChangedCount;

    @Field("total_new_serious")
    @JsonProperty("total_new_serious")
    private int totalNewSerious;

    @Field("total_new_normal")
    @JsonProperty("total_new_normal")
    private int totalNewNormal;

    @Field("total_new_prompt")
    @JsonProperty("total_new_prompt")
    private int totalNewPrompt;

    @Field("author_list")
    @JsonProperty("author_list")
    private List<NotRepairedAuthorEntity> authorList;

}

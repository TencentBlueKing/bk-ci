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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Codecc buildId和 蓝盾流水线build id关系实体类
 *
 * @version V1.0
 * @date 2020/08/20
 */
@Data
@Document(collection = "t_build_id_relationship")
@AllArgsConstructor
public class BuildIdRelationshipEntity {

    @Field("task_id")
    @Indexed(background = true)
    private Long taskId;

    @Field("codecc_build_id")
    @Indexed(background = true)
    private String codeccBuildId;

    @Field("pipeline_id")
    private String pipelineId;

    @Field("build_id")
    @Indexed(background = true)
    private String buildId;

    @Field("build_num")
    @Indexed(background = true)
    private String buildNum;

    @Field("status")
    private Integer status;

    @Field("scan_err_code")
    private TaskFailRecordEntity taskFailRecordEntity;

    @Field("elapse_time")
    private Long elapseTime;

    @Field("commit_id")
    private String commitId;

    @Field("first_trigger")
    private Boolean firstTrigger;
}

/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.task.model;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 分析次数统计实体对象
 * 每天统计一次
 *
 * @version V1.0
 * @date 2021/1/6
 */

@Data
@Document(collection = "t_analyze_count_stat")
public class AnalyzeCountStatEntity {

    /**
     * 统计日期
     */
    @Field
    @Indexed
    private String date;

    /**
     * 统计数据来源: 开源/非开源(enum DefectStatType)
     */
    @Field("data_from")
    @Indexed(background = true)
    private String dataFrom;

    /**
     * 工具名
     */
    @Field("tool_name")
    @Indexed(background = true)
    private String toolName;

    /**
     * 状态：区分统计分析成功或失败的分析状态
     * 0：分析成功   1：分析失败
     */
    @Field
    @Indexed(background = true)
    private Integer status;

    /**
     * 统计次数,一个任务ID表示一次
     */
    @Field("task_id_list")
    private List<Long> taskIdList;

}

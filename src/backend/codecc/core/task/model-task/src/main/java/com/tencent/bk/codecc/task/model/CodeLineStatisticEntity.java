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

/**
 * 代码行统计实体类
 *
 * @version V1.0
 * @date 2020/12/28
 */

@Data
@Document(collection = "t_code_line_statistic")
public class CodeLineStatisticEntity {

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
     * 总代码行
     */
    @Field("sum_code")
    private long sumCode;

    /**
     * 每日代码行
     */
    @Field("daily_code")
    private long dailyCode;

    /**
     * 每日空行
     */
    @Field("daily_blank")
    private long dailyBlank;

    /**
     * 每日注释行
     */
    @Field("daily_comment")
    private long dailyComment;

}

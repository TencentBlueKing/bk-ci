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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 灰度报告子表
 * 
 * @date 2021/1/7
 * @version V1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GrayToolReportSubEntity 
{
    /**
     * 灰度总数
     */
    @Field("gray_num")
    private Integer grayNum;

    /**
     * 总执行次数
     */
    @Field("total_num")
    private Integer totalNum;

    /**
     * 成功执行次数
     */
    @Field("success_num")
    private Integer successNum;

    /**
     * 告警数
     */
    @Field("defect_count")
    private Integer defectCount;

    /**
     * 扫描时长
     */
    @Field("elapsed_time")
    private Long elapsedTime;

}

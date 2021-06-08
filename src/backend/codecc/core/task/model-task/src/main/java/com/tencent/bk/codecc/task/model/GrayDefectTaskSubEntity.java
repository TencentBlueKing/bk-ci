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
 * 有扫描出告警的任务清单信息
 *
 * @date 2021/2/22
 * @version V1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GrayDefectTaskSubEntity {
    /**
     * 任务id
     */
    @Field("task_id")
    private Long taskId;

    /**
     * 上次告警数
     */
    @Field("last_defect_count")
    private Integer lastDefectCount;

    /**
     * 上次耗时
     */
    @Field("last_elapsed_time")
    private Long lastElapsedTime;

    /**
     * 本次告警数
     */
    @Field("current_defect_count")
    private Integer currentDefectCount;

    /**
     * 本次耗时
     */
    @Field("current_elapsed_time")
    private Long currentElapsedTime;

    /**
     * 本次是否成功
     */
    @Field("success")
    private Boolean success;

    /**
     * 工蜂
     */
    @Field("git_url")
    private String gitUrl;
}

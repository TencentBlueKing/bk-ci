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

package com.tencent.bk.codecc.apiquery.defect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 告警分组统计对象
 *
 * @version V4.0
 * @date 2020/6/15
 */
@Data
public class DefectStatModel {
    @JsonProperty("task_id")
    private Long taskId;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("severity")
    private Integer severity;

    @JsonProperty("count")
    private int count;

}

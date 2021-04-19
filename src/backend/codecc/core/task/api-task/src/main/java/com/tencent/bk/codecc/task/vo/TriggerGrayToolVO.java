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
 
package com.tencent.bk.codecc.task.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

/**
 * 触发灰度工具返回值
 * 
 * @date 2021/1/15
 * @version V1.0
 */
@Data
@AllArgsConstructor
public class TriggerGrayToolVO
{
    /**
     * 对应的codecc构建id
     */
    private String codeccBuildId;

    /**
     * 触发的任务id清单
     */
    private Set<Long> triggerTaskList;
}

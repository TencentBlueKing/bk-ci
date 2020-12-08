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
 
package com.tencent.bk.codecc.task.pojo;

import lombok.Data;

/**
 * 单个图标的抽象类
 * 
 * @date 2019/11/18
 * @version V1.0
 */
@Data
public class AbstractSingleChartModel 
{
    /**
     * 图表展示的最高数量
     */
    private int maxHeight;

    /**
     * 图表展示的最低数量
     */
    private int minHeight;
}

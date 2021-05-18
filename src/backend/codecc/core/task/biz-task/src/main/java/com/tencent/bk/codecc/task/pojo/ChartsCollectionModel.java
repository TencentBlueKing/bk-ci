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
 * 每个工具要展示的图标集合
 * 
 * @date 2019/11/18
 * @version V1.0
 */
@Data
public class ChartsCollectionModel 
{
    /**
     * C1待修复告警趋势
     */
    private AbstractSingleChartModel c1;

    /**
     * C2待修复告警作者分布
     */
    private AbstractSingleChartModel c2;

    /**
     * C3每日新增告警
     */
    private AbstractSingleChartModel c3;

    /**
     * C4每日关闭/修复告警
     */
    private AbstractSingleChartModel c4;

    /**
     * C5开源规范告警作者分布
     */
    private AbstractSingleChartModel c5;
}

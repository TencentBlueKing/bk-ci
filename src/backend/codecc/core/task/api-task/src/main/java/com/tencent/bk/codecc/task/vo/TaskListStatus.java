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

/**
 * 任务清单状态查询
 * 
 * @date 2020/2/14
 * @version V1.0
 */
public enum TaskListStatus
{

    SUCCESS("成功"),

    FAIL("失败"),

    WAITING("待分析"),

    ANALYSING("分析中"),

    DISABLED("已停用");

    private String statusName;


    TaskListStatus(String statusName)
    {
        this.statusName = statusName;
    }
}

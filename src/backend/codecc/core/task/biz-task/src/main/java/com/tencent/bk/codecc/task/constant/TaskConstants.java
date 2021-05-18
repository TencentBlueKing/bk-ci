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

package com.tencent.bk.codecc.task.constant;

/**
 * 任务模块的常量
 *
 * @version V1.0
 * @date 2019/4/23
 */
public interface TaskConstants
{
    /**
     * 1073741824表示的是其他语言  2^32
     */
    long OTHER_LANG = 1073741824L;

    /**
     * 上报分析步骤的状态标记,包括成功、失败、进行中、中断
     */
    int TASK_FLAG_SUCC = 1;
    int TASK_FLAG_FAIL = 2;
    int TASK_FLAG_PROCESSING = 3;
    int TASK_FLAG_ABORT = 4;
    /**
     * 任务的屏蔽路径树Key
     */
    String TASK_TREE_FILTER_PATH_KEY = "TASK:TREE:";

    /**
     * 任务状态，0-启用，1-停用
     */
    enum TaskStatus
    {
        ENABLE(0),
        DISABLE(1);

        private Integer value;

        TaskStatus(Integer value)
        {
            this.value = value;
        }

        public Integer value()
        {
            return value;
        }
    }

    /**
     * 工具处理模式
     */
    enum ToolPattern {
        LINT,
        COVERITY,
        KLOCWORK,
        CCN,
        DUPC,
        STAT,
        TSCLUA;
    }


    /**
     * 工具跟进状态: 0/1-未跟进，2-体验，3-接入中，4-已接入，5-挂起，6-下架/停用
     */
    enum FOLLOW_STATUS
    {
        NOT_FOLLOW_UP_0(0),
        NOT_FOLLOW_UP_1(1),
        EXPERIENCE(2),
        ACCESSING(3),
        ACCESSED(4),
        HANG_UP(5),
        WITHDRAW(6);

        private int value;

        FOLLOW_STATUS(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }
    }

    /**
     * 任务状态，0-启用，1-停用
     */
    enum PlatformStatus
    {
        EFFECTIVE(0),
        invalid(1);

        private Integer value;

        PlatformStatus(Integer value)
        {
            this.value = value;
        }

        public Integer value()
        {
            return value;
        }
    }


    /**
     * 编译行工具
     */
    enum CompileTool
    {
        COVERITY,
        KLOCWORK,
        SPOTBUGS
    }

    /**
     * 工蜂任务检查方式
     */
    enum GongfengCheckType
    {
        SCHEDULE_TASK,
        CUSTOM_TRIGGER_TASK
    }


}

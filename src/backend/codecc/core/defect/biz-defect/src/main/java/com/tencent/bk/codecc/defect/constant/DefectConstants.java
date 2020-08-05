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

package com.tencent.bk.codecc.defect.constant;

/**
 * 告警模块的常量
 *
 * @version V1.0
 * @date 2019/4/23
 */
public interface DefectConstants
{
    /**
     * 严重程度类别：严重（1），一般（2），提示（4）
     */
    enum DefectSeverity
    {
        SERIOUS(1),
        NORMAL(2),
        PROMPT(4);

        private int value;

        DefectSeverity(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }
    }


    enum CheckerPackageStatus
    {
        CLOSE(1),
        OPEN(2);

        private Integer value;

        CheckerPackageStatus(Integer value)
        {
            this.value = value;
        }

        public Integer getValue()
        {
            return value;
        }

    }


    enum CheckerStatus
    {
        CLOSE(1),
        OPEN(2);

        private Integer value;

        CheckerStatus(Integer value)
        {
            this.value = value;
        }

        public Integer getValue()
        {
            return value;
        }

    }

    /**
     * 重复代码块类型，source表示源代码块，target表示目标代码块
     */
    enum DupcCodeBlockType
    {
        SOURCE("source"),
        TARGET("target");

        private String value;

        DupcCodeBlockType(String value)
        {
            this.value = value;
        }

        public String value()
        {
            return value;
        }
    }

    /**
     * 更新工具删除文件列表的类型
     */
    enum UpdateToolDeleteFileType
    {
        ADD,
        REMOVE
    }


    /**
     * CodeCC创建任务元素类型
     */
    String LINUX_CODECC_SCRIPT = "linuxCodeCCScript";

    /**
     * 蓝盾流水线创建任务元素类型
     */
    String LINUX_PAAS_CODECC_SCRIPT = "linuxPaasCodeCCScript";


    /**
     * -1表示该元数据无效，应被红线拦截
     */
    long FORBIDDEN_COUNT = -1L;

    /**
     * -1表示该元数据无效，应被红线拦截
     */
    double FORBIDDEN_COUNT_F = -1.0D;

    /**
     * 0表示该元数据不需要被拦截
     */
    long PASS_COUNT = 0L;


}

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

package com.tencent.bk.codecc.defect.utils;

import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.Step4Cov;
import com.tencent.devops.common.constant.ComConstants.StepFlag;

import java.util.List;

/**
 * 代码语言转换
 *
 * @version V1.0
 * @date 2019/11/18
 */

public class ConvertUtil
{

    /**
     * 将代码语言类型转换成前端展示的字符, modify:去除最后分号
     *
     * @param codeLangParam 语言类型key参数
     * @param metadataList  语言类型元数据
     * @return langA;langB
     */
    public static String convertCodeLang(Long codeLangParam, List<MetadataVO> metadataList)
    {
        StringBuilder codeLang = new StringBuilder();

        if (codeLangParam != null)
        {
            for (MetadataVO metadata : metadataList)
            {
                Integer langValue = Integer.valueOf(metadata.getKey());
                if ((langValue & codeLangParam) != 0)
                {
                    codeLang.append(metadata.getName()).append(ComConstants.SEMICOLON);
                }
            }
            if (codeLang.length() > 0)
            {
                codeLang.deleteCharAt(codeLang.length() - 1);
            }
        }

        return codeLang.toString();
    }

    /**
     * 接入Coverity工具步骤
     *
     * @param step 阶段
     * @return description
     */
    public static String convertStep4Cov(int step)
    {
        String strStep = "";
        if (step == Step4Cov.READY.value())
        {
            strStep = "接入";
        }
        else if (step == Step4Cov.UPLOAD.value())
        {
            strStep = "上传";
        }
        else if (step == Step4Cov.QUEUE.value())
        {
            strStep = "排队";
        }
        else if (step == Step4Cov.ANALYZE.value())
        {
            strStep = "分析";
        }
        else if (step == Step4Cov.COMMIT.value())
        {
            strStep = "缺陷提交";
        }
        else if (step == Step4Cov.DEFECT_SYNS.value())
        {
            strStep = "告警同步";
        }
        else if (step == Step4Cov.COMPLETE.value())
        {
            strStep = "分析";
        }
        return strStep;
    }

    /**
     * 上报分析步骤的状态标记,包括成功、失败、进行中、中断
     *
     * @param flag 标记
     * @return description
     */
    public static String getStepFlag(int flag) {
        String strFlag = "";
        if (flag == StepFlag.SUCC.value())
        {
            strFlag = "成功";
        }
        else if (flag == StepFlag.FAIL.value())
        {
            strFlag = "失败";
        }
        else if (flag == StepFlag.PROCESSING.value())
        {
            strFlag = "进行中";
        }
        else if (flag == StepFlag.ABORT.value())
        {
            strFlag = "中断";
        }
        return strFlag;
    }
}

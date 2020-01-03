/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.model.LintStatisticEntity;
import com.tencent.bk.codecc.defect.service.IQueryStatisticBizService;
import com.tencent.devops.common.api.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.LintLastAnalysisResultVO;
import com.tencent.devops.common.api.ToolLastAnalysisResultVO;
import com.tencent.devops.common.constant.ComConstants;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Lint类查询分析统计结果的业务逻辑类
 *
 * @version V1.0
 * @date 2019/6/8
 */
@Service("LINTQueryStatisticBizService")
public class LintQueryStatisticBizServiceImpl implements IQueryStatisticBizService
{
    @Autowired
    private LintStatisticRepository lintStatisticRepository;

    @Override
    public BaseLastAnalysisResultVO processBiz(ToolLastAnalysisResultVO arg)
    {
        long taskId = arg.getTaskId();
        String toolName = arg.getToolName();

        LintStatisticEntity statisticEntity = lintStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName);
        LintLastAnalysisResultVO lastAnalysisResultVO = new LintLastAnalysisResultVO();
        if (statisticEntity != null)
        {
            BeanUtils.copyProperties(statisticEntity, lastAnalysisResultVO);
        }
        lastAnalysisResultVO.setPattern(ComConstants.ToolPattern.LINT.name());
        return lastAnalysisResultVO;
    }
}

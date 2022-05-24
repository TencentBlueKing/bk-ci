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

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildStackRepository;
import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.service.IQueryStatisticBizService;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.CCNLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.CCNNotRepairedAuthorVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.api.clusterresult.CcnClusterResultVO;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * CCN类查询分析统计结果的业务逻辑类
 *
 * @version V1.0
 * @date 2019/6/8
 */
@Service("CCNQueryStatisticBizService")
public class CCNQueryStatisticBizServiceImpl implements IQueryStatisticBizService
{
    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;

    @Autowired
    @Qualifier("CCN")
    private AbstractClusterDefectService ccnClusterDefectService;

    @Autowired
    private ToolBuildStackRepository toolBuildStackRepository;

    @Override
    public BaseLastAnalysisResultVO processBiz(ToolLastAnalysisResultVO arg, boolean isLast) {
        long taskId = arg.getTaskId();
        String toolName = arg.getToolName();
        String buildId = arg.getBuildId();

        CCNStatisticEntity statisticEntity = isLast
                ? ccnStatisticRepository.findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId, toolName)
                : ccnStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);

        CCNLastAnalysisResultVO lastAnalysisResultVO = new CCNLastAnalysisResultVO();
        if (statisticEntity != null) {
            BeanUtils.copyProperties(statisticEntity, lastAnalysisResultVO);

            if (!CollectionUtils.isEmpty(statisticEntity.getNewAuthorStatistic())) {
                lastAnalysisResultVO.setNewAuthorStatistic(
                        statisticEntity.getNewAuthorStatistic().stream().map(source -> {
                            CCNNotRepairedAuthorVO newAuthorVO = new CCNNotRepairedAuthorVO();
                            BeanUtils.copyProperties(source, newAuthorVO);
                            return newAuthorVO;
                        }).collect(Collectors.toList())
                );
            }

            if (!CollectionUtils.isEmpty(statisticEntity.getExistAuthorStatistic())) {
                lastAnalysisResultVO.setExistAuthorStatistic(
                        statisticEntity.getExistAuthorStatistic().stream().map(source -> {
                            CCNNotRepairedAuthorVO existAuthorVO = new CCNNotRepairedAuthorVO();
                            BeanUtils.copyProperties(source, existAuthorVO);
                            return existAuthorVO;
                        }).collect(Collectors.toList())
                );
            }

            if (isLast) {
                setAverageThousandDefect(lastAnalysisResultVO, taskId, buildId);
            }
        }

        lastAnalysisResultVO.setPattern(ComConstants.ToolPattern.CCN.name());

        return lastAnalysisResultVO;
    }

    /**
     * 设置千行超标复杂度相关数据
     *
     * @param lastAnalysisResultVO
     * @param taskId
     * @param buildId
     */
    private void setAverageThousandDefect(CCNLastAnalysisResultVO lastAnalysisResultVO, long taskId, String buildId) {
        CcnClusterResultVO clusterStatistic =
                (CcnClusterResultVO) ccnClusterDefectService.getClusterStatistic(taskId, buildId);

        if (clusterStatistic == null) {
            lastAnalysisResultVO.setAverageThousandDefect(Double.valueOf(0));
            lastAnalysisResultVO.setAverageThousandDefectChange(Double.valueOf(0));
            return;
        }

        lastAnalysisResultVO.setAverageThousandDefect(clusterStatistic.getAverageThousandDefect());
        lastAnalysisResultVO.setAverageThousandDefectChange(clusterStatistic.getAverageThousandDefectChange());
    }
}

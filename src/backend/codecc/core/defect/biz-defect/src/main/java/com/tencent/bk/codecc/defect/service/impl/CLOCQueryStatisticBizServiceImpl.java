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
 
package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCStatisticRepository;
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.service.IQueryStatisticBizService;
import com.tencent.bk.codecc.defect.vo.CLOCDefectQueryRspInfoVO;
import com.tencent.devops.common.api.analysisresult.BaseLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.CLOCLastAnalysisResultVO;
import com.tencent.devops.common.api.analysisresult.ToolLastAnalysisResultVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * cloc查询服务类
 * 
 * @date 2019/11/1
 * @version V1.0
 */
@Slf4j
@Service("CLOCQueryStatisticBizService")
public class CLOCQueryStatisticBizServiceImpl implements IQueryStatisticBizService
{
    @Autowired
    CLOCStatisticRepository clocStatisticRepository;

    @Autowired
    CLOCDefectRepository clocDefectRepository;

    @Override
    public BaseLastAnalysisResultVO processBiz(ToolLastAnalysisResultVO arg, boolean isLast) {
        log.info("get cloc statistic list, taskId: {} | buildId: {}", arg.getTaskId(), arg.getBuildId());
        String buildId = arg.getBuildId();

        List<CLOCStatisticEntity> clocStatisticEntityList =
                clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(arg.getTaskId(), arg.getToolName(), buildId);

        long sumCode = 0;
        long sumBlank = 0;
        long sumComment = 0;
        long codeChange = 0;
        long blankChange = 0;
        long commentChange = 0;
        long fileNum = 0;
        long fileNumChange = 0;

        for (CLOCStatisticEntity clocStatisticEntity : clocStatisticEntityList
             ) {
            if (clocStatisticEntity.getSumCode() != null) {
                sumBlank += clocStatisticEntity.getSumBlank();
                sumCode += clocStatisticEntity.getSumCode();
                sumComment += clocStatisticEntity.getSumComment();
            }

            if (clocStatisticEntity.getCodeChange() != null) {
                blankChange += clocStatisticEntity.getBlankChange();
                codeChange += clocStatisticEntity.getCodeChange();
                commentChange += clocStatisticEntity.getCommentChange();
            }

            if (clocStatisticEntity.getFileNum() != null) {
                fileNum += clocStatisticEntity.getFileNum();
                fileNumChange += clocStatisticEntity.getFileNumChange();
            }
        }

        CLOCLastAnalysisResultVO clocLastAnalysisResultVO = new CLOCLastAnalysisResultVO();
        clocLastAnalysisResultVO.setSumCode(sumCode);
        clocLastAnalysisResultVO.setSumBlank(sumBlank);
        clocLastAnalysisResultVO.setSumComment(sumComment);
        clocLastAnalysisResultVO.setTotalLines(sumBlank + sumCode + sumComment);
        clocLastAnalysisResultVO.setCodeChange(codeChange);
        clocLastAnalysisResultVO.setBlankChange(blankChange);
        clocLastAnalysisResultVO.setCommentChange(commentChange);
        clocLastAnalysisResultVO.setLinesChange(blankChange + codeChange + commentChange);
        clocLastAnalysisResultVO.setFileNum(fileNum);
        clocLastAnalysisResultVO.setFileNumChange(fileNumChange);
        clocLastAnalysisResultVO.setPattern(String.valueOf(ComConstants.Tool.CLOC));

        return clocLastAnalysisResultVO;
    }

}

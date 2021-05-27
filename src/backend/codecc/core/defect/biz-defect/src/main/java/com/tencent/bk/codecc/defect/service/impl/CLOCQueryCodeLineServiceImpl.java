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

import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CLOCDefectDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CLOCStatisticsDao;
import com.tencent.bk.codecc.defect.dto.CodeLineModel;
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.service.ICLOCQueryCodeLineService;
import com.tencent.bk.codecc.defect.vo.CLOCDefectQueryRspInfoVO;
import com.tencent.bk.codecc.defect.vo.ToolClocRspVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * cloc查询代码行数服务
 * 
 * @date 2020/3/31
 * @version V1.0
 */
@Service
public class CLOCQueryCodeLineServiceImpl implements ICLOCQueryCodeLineService
{
    @Autowired
    private CLOCDefectDao clocDefectDao;
    @Autowired
    private CLOCStatisticsDao clocStatisticsDao;
    @Autowired
    private CLOCStatisticRepository clocStatisticRepository;
    @Override
    public ToolClocRspVO getCodeLineInfo(Long taskId, String toolName)
    {
        List<CodeLineModel> codeLineModelList = clocDefectDao.getCodeLineInfo(taskId, toolName);
        ToolClocRspVO toolClocRspVO = new ToolClocRspVO();
        toolClocRspVO.setTaskId(taskId);
        toolClocRspVO.setCodeLineList(codeLineModelList);
        return toolClocRspVO;
    }

    /**
     * 按任务ID统计总代码行数
     *
     * @param taskIds 任务ID集合
     * @return int
     */
    @Override
    public Long queryCodeLineByTaskIds(Collection<Long> taskIds) {
        long codeLine = 0;
        if (CollectionUtils.isEmpty(taskIds)) {
            return codeLine;
        }
        List<CLOCStatisticEntity> entityList = clocStatisticsDao.queryLastBuildIdByTaskIds(taskIds);
        if (CollectionUtils.isEmpty(entityList)) {
            return codeLine;
        }
        List<String> buildIds = entityList.stream().map(CLOCStatisticEntity::getBuildId).collect(Collectors.toList());

        List<CLOCStatisticEntity> statisticEntityList =
                clocStatisticsDao.batchStatClocStatisticByTaskId(taskIds, buildIds);

        return statisticEntityList.stream().map(item -> item.getSumCode() + item.getSumBlank() + item.getSumComment())
                        .reduce(Long::sum).orElse(0L);
    }

    @Override
    public CLOCDefectQueryRspInfoVO generateSpecificLanguage(long taskId, String toolName, String language)
    {
        CLOCStatisticEntity clocStatisticEntity = clocStatisticRepository
                .findFirstByTaskIdAndToolNameAndLanguageOrderByUpdatedDateDesc(taskId, toolName, language);
        CLOCDefectQueryRspInfoVO clocDefectQueryRspInfoVO = new CLOCDefectQueryRspInfoVO();
        if (null != clocStatisticEntity)
        {
            clocDefectQueryRspInfoVO.setLanguage(language);
            clocDefectQueryRspInfoVO.setSumCode(clocStatisticEntity.getSumCode());
            clocDefectQueryRspInfoVO.setSumBlank(clocStatisticEntity.getSumBlank());
            clocDefectQueryRspInfoVO.setSumComment(clocStatisticEntity.getSumComment());
        }
        return clocDefectQueryRspInfoVO;
    }
}

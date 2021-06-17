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

package com.tencent.bk.codecc.apiquery.service.impl;


import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CLOCStatisticsDao;
import com.tencent.bk.codecc.apiquery.defect.model.CLOCStatisticModel;
import com.tencent.bk.codecc.apiquery.defect.model.TaskLogOverviewModel;
import com.tencent.bk.codecc.apiquery.service.ICLOCQueryCodeLineService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * cloc查询代码行数服务
 *
 * @version V1.0
 * @date 2020/3/31
 */
@Service
public class CLOCQueryCodeLineServiceImpl implements ICLOCQueryCodeLineService {

    @Autowired
    private CLOCStatisticsDao clocStatisticsDao;

    @Autowired
    private TaskLogOverviewServiceImpl taskLogOverviewService;

    /**
     * 按任务ID统计总代码行数
     *
     * @param taskIds 任务ID集合
     * @return int
     */
    @Override
    public Long queryCodeLineByTaskIds(Collection<Long> taskIds) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return 0L;
        }
        List<CLOCStatisticModel> statisticEntityList = queryTaskLastCodeLine(taskIds);

        return statisticEntityList.stream().map(item -> item.getSumCode() + item.getSumBlank() + item.getSumComment())
                        .reduce(Long::sum).orElse(0L);
    }

    /**
     * 按任务ID统计各任务的代码行数
     *
     * @param taskIds 任务ID集合
     * @return int
     */
    @Override
    public List<CLOCStatisticModel> queryTaskLastCodeLine(Collection<Long> taskIds) {
        List<TaskLogOverviewModel> entityList = taskLogOverviewService.findLatestAnalyzeStatus(taskIds, null);

        if (CollectionUtils.isEmpty(entityList)) {
            return Lists.newArrayList();
        }
        List<String> buildIds = entityList.stream().map(TaskLogOverviewModel::getBuildId).collect(Collectors.toList());
        return clocStatisticsDao.batchStatClocStatisticByTaskId(taskIds, buildIds);
    }

}

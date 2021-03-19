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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CheckerSetDao;
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetModel;
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetTaskRelationshipModel;
import com.tencent.bk.codecc.apiquery.service.ICheckerSetBizService;
import com.tencent.bk.codecc.apiquery.vo.CheckerSetVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 规则集业务实现类
 *
 * @version V2.0
 * @date 2020/5/13
 */
@Slf4j
@Service
public class CheckerSetBizServiceImpl implements ICheckerSetBizService
{
    @Autowired
    private CheckerSetDao checkerSetDao;


    @Override
    public Map<Long, List<CheckerSetVO>> getCheckerSetByTaskIdSet(Collection<Long> taskIds)
    {
        Map<Long, List<CheckerSetVO>> taskCheckerSetMap = Maps.newHashMap();

        if (CollectionUtils.isNotEmpty(taskIds))
        {
            List<CheckerSetTaskRelationshipModel> relationshipModels = checkerSetDao.findByTaskId(taskIds);

            if (CollectionUtils.isNotEmpty(relationshipModels))
            {
                // 按规则集ID分组 并收集任务ID
                Map<String, List<Long>> checkerSetId4TaskIdsMap = relationshipModels.stream().collect(Collectors
                        .groupingBy(CheckerSetTaskRelationshipModel::getCheckerSetId,
                                Collectors.mapping(CheckerSetTaskRelationshipModel::getTaskId, Collectors.toList())));

                List<CheckerSetModel> checkerSetModels =
                        checkerSetDao.findByCheckerSetIdList(checkerSetId4TaskIdsMap.keySet(), false);

                // 将规则集按任务ID分配
                checkerSetModels.forEach(checkerSetModel ->
                {
                    String checkerSetId = checkerSetModel.getCheckerSetId();
                    List<Long> taskIdList = checkerSetId4TaskIdsMap.get(checkerSetId);
                    if (CollectionUtils.isNotEmpty(taskIdList))
                    {
                        CheckerSetVO checkerSetVO = new CheckerSetVO();
                        BeanUtils.copyProperties(checkerSetModel, checkerSetVO);
                        taskIdList.forEach(taskId ->
                        {
                            List<CheckerSetVO> modelList =
                                    taskCheckerSetMap.computeIfAbsent(taskId, v -> Lists.newArrayList());
                            modelList.add(checkerSetVO);
                        });
                    }
                });
            }
        }
        return taskCheckerSetMap;
    }

}

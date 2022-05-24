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
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CheckerSetRelationshipDao;
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetModel;
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetProjectRelationshipModel;
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetTaskRelationshipModel;
import com.tencent.bk.codecc.apiquery.service.ICheckerSetBizService;
import com.tencent.bk.codecc.apiquery.service.MetaDataService;
import com.tencent.bk.codecc.apiquery.utils.ConvertUtil;
import com.tencent.bk.codecc.apiquery.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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

    @Autowired
    private CheckerSetRelationshipDao checkerSetRelationshipDao;

    @Autowired
    private MetaDataService metaDataService;


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


    private boolean filterCheckerSetEntity(CheckerSetModel checkerSetModel,
            Map<String, CheckerSetProjectRelationshipModel> checkerSetRelationshipMap) {
        String checkerSetId = checkerSetModel.getCheckerSetId();
        if (checkerSetRelationshipMap.get(checkerSetId) == null) {
            return false;
        }
        if (checkerSetModel.getVersion() == null) {
            return false;
        }
        return checkerSetModel.getVersion().equals(checkerSetRelationshipMap.get(checkerSetId).getVersion());
    }


    /**
     * 根据任务Id查询任务已经关联的规则集列表
     *
     * @param taskId 任务ID
     * @return list
     */
    @Override
    public List<CheckerSetVO> getCheckerSetsByTaskId(Long taskId) {
        // 查出任务维度的id集合
        List<CheckerSetTaskRelationshipModel> checkerSetTaskRelationshipModelList =
                checkerSetDao.findByTaskId(Lists.newArrayList(taskId));
        if (CollectionUtils.isEmpty(checkerSetTaskRelationshipModelList))
        {
            return new ArrayList<>();
        }
        Set<String> checkerSetIds = checkerSetTaskRelationshipModelList.stream()
                .map(CheckerSetTaskRelationshipModel::getCheckerSetId).collect(Collectors.toSet());
        String projectId = checkerSetTaskRelationshipModelList.get(0).getProjectId();

        List<CheckerSetProjectRelationshipModel> checkerSetProjectRelationshipModels =
                checkerSetRelationshipDao.findByCheckerSetIdInAndProjectId(checkerSetIds, projectId);
        // 计算规则集的使用量
        Map<String, Long> checkerSetCountMap = checkerSetTaskRelationshipModelList.stream().collect(
                Collectors.groupingBy(CheckerSetTaskRelationshipModel::getCheckerSetId, Collectors.counting()));

        Map<String, CheckerSetProjectRelationshipModel> checkerSetRelationshipMap =
                checkerSetProjectRelationshipModels.stream().collect(Collectors
                        .toMap(CheckerSetProjectRelationshipModel::getCheckerSetId, Function.identity(), (k, v) -> v));

        List<CheckerSetModel> checkerSetModels = checkerSetDao.findByCheckerSetIdList(checkerSetIds, true);

        // 按版本过滤，按使用量排序
        return checkerSetModels.stream()
                .filter(checkerSetModel -> filterCheckerSetEntity(checkerSetModel, checkerSetRelationshipMap))
                .sorted(Comparator.comparingLong(o -> checkerSetCountMap.containsKey(o.getCheckerSetId())
                        ? -checkerSetCountMap.get(o.getCheckerSetId()) : 0L)).map(checkerSetModel -> {
                    CheckerSetVO
                            checkerSetVO = new CheckerSetVO();
                    BeanUtils.copyProperties(checkerSetModel, checkerSetVO);
                    Integer useCount = checkerSetCountMap.get(checkerSetVO.getCheckerSetId()) == null ? 0 :
                            Integer.parseInt(checkerSetCountMap.get(checkerSetVO.getCheckerSetId()).toString());
                    checkerSetVO.setTaskUsage(useCount);
                    if (CollectionUtils.isNotEmpty(checkerSetModel.getCheckerProps())) {
                        Set<String> toolList = new HashSet<>();
                        checkerSetVO
                                .setCheckerProps(checkerSetModel.getCheckerProps().stream().map(checkerPropsModel -> {
                                    toolList.add(checkerPropsModel.getToolName());
                                    CheckerPropVO checkerPropVO = new CheckerPropVO();
                                    BeanUtils.copyProperties(checkerPropsModel, checkerPropVO);
                                    return checkerPropVO;
                                }).collect(Collectors.toList()));
                        checkerSetVO.setToolList(toolList);
                    }

                    CheckerSetProjectRelationshipModel projectRelationshipModel =
                            checkerSetRelationshipMap.get(checkerSetModel.getCheckerSetId());

                    if ((projectRelationshipModel != null && null != projectRelationshipModel.getDefaultCheckerSet()
                            && projectRelationshipModel.getDefaultCheckerSet()) || (CheckerSetSource.DEFAULT.name().
                            equals(checkerSetModel.getCheckerSetSource()) && null == projectRelationshipModel)) {
                        checkerSetVO.setDefaultCheckerSet(true);
                    } else {
                        checkerSetVO.setDefaultCheckerSet(false);
                    }

                    return checkerSetVO;
                }).collect(Collectors.toList());
    }

    /**
     * 分页查询规则管理列表O
     *
     * @param checkerSetListQueryReq 规则集管理列表请求体
     * @param pageable               分页数据
     * @return page
     */
    @Override
    public Page<CheckerSetModel> getCheckerSetList(CheckerSetListQueryReq checkerSetListQueryReq, Pageable pageable) {
        return checkerSetDao.getCheckerSetList(checkerSetListQueryReq, pageable);
    }
}

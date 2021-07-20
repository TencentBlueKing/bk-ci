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

import com.google.common.collect.Maps;
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.MetricsDao;
import com.tencent.bk.codecc.apiquery.defect.model.MetricsModel;
import com.tencent.bk.codecc.apiquery.service.MetricsService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 度量服务接口实现
 *
 * @version V1.0
 * @date 2021/3/23
 */

@Service
public class MetricsServiceImpl implements MetricsService {

    @Autowired
    private MetricsDao metricsDao;


    /**
     * 获取任务度量数据映射
     *
     * @param taskIds  任务id集合
     * @param buildIds 构建id集合
     * @return map
     */
    @Override
    public Map<Long, MetricsModel> getTaskMetricsMap(Collection<Long> taskIds, Collection<String> buildIds) {

        if (CollectionUtils.isEmpty(taskIds) || CollectionUtils.isEmpty(buildIds)) {
            return Maps.newHashMap();
        }

        List<MetricsModel> metricsModelList = metricsDao.findByTaskIdAndBuildId(taskIds, buildIds);
        return metricsModelList.stream().distinct()
                .collect(Collectors.toMap(MetricsModel::getTaskId, Function.identity(), (k, v) -> v));
    }

}

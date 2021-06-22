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

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.service.FilterPathService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 屏蔽路径服务
 *
 * @version V1.0
 * @date 2020/9/23
 */
@Slf4j
@Service
public class FilterPathServiceImpl implements FilterPathService {
    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    /**
     * 设置屏蔽路径
     * <p>
     * 对于.code.yml中的测试代码，也要进行代码规范检查
     *
     * @param taskVO
     * @param toolName
     * @return
     */
    @Override
    public Set<String> getFilterPaths(TaskDetailVO taskVO, String toolName) {
        Boolean scanTestSource = taskVO.getScanTestSource();
        if (scanTestSource != null && scanTestSource) {
            List<BaseDataVO> baseDataVOList = thirdPartySystemCaller
                    .getParamsByType(ComConstants.BaseConfig.STANDARD_TOOLS.name());
            if (CollectionUtils.isNotEmpty(baseDataVOList)) {
                BaseDataVO standardToolsVO = baseDataVOList.get(0);
                if (StringUtils.isNotEmpty(standardToolsVO.getParamValue())) {
                    Set<String> standardToolSet = Sets.newHashSet(
                            standardToolsVO.getParamValue().split(ComConstants.STRING_SPLIT));
                    if (standardToolSet.contains(toolName)) {
                        log.info("standard tool filter path: {}, {}", taskVO.getTaskId(), toolName);
                        return getFilterPathsFromTask(taskVO, true);
                    }
                }
            }
        }
        return getFilterPathsFromTask(taskVO, false);
    }

    /**
     * 获取任务的所有
     *
     * @param taskVO
     * @param scanTestSource
     * @return
     */
    public Set<String> getFilterPathsFromTask(TaskDetailVO taskVO, boolean scanTestSource) {
        Set<String> filterPath = new HashSet<>();

        if (CollectionUtils.isNotEmpty(taskVO.getFilterPath())) {
            filterPath.addAll(taskVO.getFilterPath());
        }
        if (CollectionUtils.isNotEmpty(taskVO.getDefaultFilterPath())) {
            filterPath.addAll(taskVO.getDefaultFilterPath());
        }
        if (CollectionUtils.isNotEmpty(taskVO.getThirdPartyFilterPath())) {
            filterPath.addAll(taskVO.getThirdPartyFilterPath());
        }
        if (CollectionUtils.isNotEmpty(taskVO.getAutoGenFilterPath())) {
            filterPath.addAll(taskVO.getAutoGenFilterPath());
        }

        // 如果不扫描，就加入到屏蔽路径里面
        if (!scanTestSource && CollectionUtils.isNotEmpty(taskVO.getTestSourceFilterPath())) {
            filterPath.addAll(taskVO.getTestSourceFilterPath());
        }
        return filterPath;
    }
}

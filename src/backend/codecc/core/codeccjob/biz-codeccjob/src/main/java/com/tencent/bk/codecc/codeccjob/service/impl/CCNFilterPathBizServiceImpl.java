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

package com.tencent.bk.codecc.codeccjob.service.impl;

import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.codeccjob.service.AbstractFilterPathBizService;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ccn类工具的路径屏蔽
 *
 * @version V1.0
 * @date 2019/11/1
 */
@Service("CCNFilterPathBizService")
@Slf4j
public class CCNFilterPathBizServiceImpl extends AbstractFilterPathBizService
{
    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Override
    public Result processBiz(FilterPathInputVO filterPathInputVO)
    {
        // CCN类屏蔽路径
        List<CCNDefectEntity> ccnFileInfoList = ccnDefectRepository.findByTaskId(filterPathInputVO.getTaskId());
        if (CollectionUtils.isNotEmpty(ccnFileInfoList))
        {
            long currTime = System.currentTimeMillis();
            List<CCNDefectEntity> needUpdateDefectList = ccnFileInfoList.stream()
                    .filter(defectEntity ->
                    {
                        String path = defectEntity.getUrl();
                        if (StringUtils.isEmpty(path)) {
                            path = defectEntity.getRelPath();
                        }
                        if (StringUtils.isEmpty(path)){
                            path = defectEntity.getFilePath();
                        }
                        if (StringUtils.isEmpty(path)) {
                            return false;
                        }
                        return PathUtils.checkIfMaskByPath(path, filterPathInputVO.getFilterPaths());
                    })
                    .collect(Collectors.toList());
            needUpdateDefectList.forEach(defectEntity ->
            {
                defectEntity.setStatus(getUpdateStatus(filterPathInputVO, defectEntity.getStatus()));
                defectEntity.setExcludeTime(currTime);
            });
            ccnDefectRepository.saveAll(needUpdateDefectList);
        }
        return new Result(CommonMessageCode.SUCCESS);
    }
}

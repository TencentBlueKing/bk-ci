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

import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.codeccjob.service.AbstractFilterPathBizService;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 通用工具的路径屏蔽
 *
 * @version V1.0
 * @date 2019/11/1
 */
@Service("CommonFilterPathBizService")
@Slf4j
public class CommonFilterPathBizServiceImpl extends AbstractFilterPathBizService
{
    @Autowired
    private DefectRepository defectRepository;

    @Override
    public Result processBiz(FilterPathInputVO filterPathInputVO)
    {
        List<DefectEntity> defectEntityList = defectRepository.findByTaskIdAndToolName(filterPathInputVO.getTaskId(), filterPathInputVO.getToolName());
        if (CollectionUtils.isNotEmpty(defectEntityList))
        {
            long currTime = System.currentTimeMillis();

            List<DefectEntity> needUpdateDefectList = new ArrayList<>();
            defectEntityList.forEach(defectEntity ->
            {
                int status = defectEntity.getStatus();
                // 告警未被修复，且命中过滤路径
                if ((status & ComConstants.DefectStatus.FIXED.value()) == 0
                        && checkIfMaskByPath(defectEntity.getFilePathname(), filterPathInputVO.getFilterPaths()))
                {
                    if (filterPathInputVO.getAddFile())
                    {
                        status = status | ComConstants.DefectStatus.PATH_MASK.value();
                        if (defectEntity.getExcludeTime() == 0)
                        {
                            defectEntity.setExcludeTime(currTime);
                        }
                    }
                    else
                    {
                        if ((status & ComConstants.DefectStatus.PATH_MASK.value()) > 0)
                        {
                            status = status - ComConstants.DefectStatus.PATH_MASK.value();
                            if (status < ComConstants.DefectStatus.PATH_MASK.value())
                            {
                                defectEntity.setExcludeTime(0);
                            }
                        }
                    }
                }

                if (defectEntity.getStatus() != status)
                {
                    defectEntity.setStatus(status);
                    needUpdateDefectList.add(defectEntity);
                }

            });

            defectRepository.saveAll(needUpdateDefectList);
        }
        return new Result(CommonMessageCode.SUCCESS);
    }

    protected Boolean checkIfMaskByPath(String filePathname, Set<String> filterPaths)
    {
        return PathUtils.checkIfMaskByPath(filePathname, filterPaths);
    }
}

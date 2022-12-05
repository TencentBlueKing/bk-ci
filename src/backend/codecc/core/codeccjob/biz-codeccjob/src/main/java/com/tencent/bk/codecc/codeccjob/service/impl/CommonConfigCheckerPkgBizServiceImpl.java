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
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.DefectRepository;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.IBizService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * lint类工具的规则配置
 *
 * @version V1.0
 * @date 2019/12/2
 */
@Service("CommonConfigCheckerPkgBizService")
@Slf4j
public class CommonConfigCheckerPkgBizServiceImpl implements IBizService<ConfigCheckersPkgReqVO>
{
    @Autowired
    private DefectRepository defectRepository;

    @Override
    public Result processBiz(ConfigCheckersPkgReqVO configCheckersPkgReqVO)
    {
        List<DefectEntity> defectList = defectRepository.findByTaskIdAndToolName(configCheckersPkgReqVO.getTaskId(), configCheckersPkgReqVO.getToolName());
        if (CollectionUtils.isNotEmpty(defectList))
        {
            long currTime = System.currentTimeMillis();
            List<String> closedCheckers = configCheckersPkgReqVO.getClosedCheckers();
            List<String> openCheckers = configCheckersPkgReqVO.getOpenedCheckers();

            List<DefectEntity> needUpdateDefectList = new ArrayList<>();
            defectList.forEach(defectEntity ->
            {
                String checkerName = defectEntity.getCheckerName();
                int status = defectEntity.getStatus();

                // 告警未被修复
                if ((status & ComConstants.DefectStatus.FIXED.value()) == 0)
                {
                    // 命中将关闭的规则
                    if (CollectionUtils.isNotEmpty(closedCheckers) && closedCheckers.contains(checkerName))
                    {
                        status = status | ComConstants.DefectStatus.CHECKER_MASK.value();
                        if (defectEntity.getExcludeTime() == 0)
                        {
                            defectEntity.setExcludeTime(currTime);
                        }
                    }
                    // 命中将打开的规则
                    else if (CollectionUtils.isNotEmpty(openCheckers) && openCheckers.contains(checkerName))
                    {
                        if ((status & ComConstants.DefectStatus.CHECKER_MASK.value()) > 0)
                        {
                            status = status - ComConstants.DefectStatus.CHECKER_MASK.value();
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
}

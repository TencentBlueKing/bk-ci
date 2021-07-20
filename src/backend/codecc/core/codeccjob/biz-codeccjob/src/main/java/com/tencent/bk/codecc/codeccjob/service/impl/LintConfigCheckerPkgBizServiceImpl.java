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

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.codeccjob.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.IBizService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * lint类工具的规则配置
 *
 * @version V1.0
 * @date 2019/12/2
 */
@Service("LINTConfigCheckerPkgBizService")
@Slf4j
public class LintConfigCheckerPkgBizServiceImpl implements IBizService<ConfigCheckersPkgReqVO>
{
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;

    @Override
    public Result processBiz(ConfigCheckersPkgReqVO configCheckersPkgReqVO)
    {
        Long taskId = configCheckersPkgReqVO.getTaskId();
        String toolName = configCheckersPkgReqVO.getToolName();
        List<String> closedCheckers = configCheckersPkgReqVO.getClosedCheckers();
        List<String> openCheckers = configCheckersPkgReqVO.getOpenedCheckers();
        Set<String> checkers = Sets.newHashSet();
        if (CollectionUtils.isNotEmpty(closedCheckers))
        {
            checkers.addAll(closedCheckers);
        }
        if (CollectionUtils.isNotEmpty(openCheckers))
        {
            checkers.addAll(openCheckers);
        }

        // 不需要查询已修复的告警
        Set<Integer> excludeStatusSet = Sets.newHashSet(ComConstants.DefectStatus.FIXED.value(),
                ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.FIXED.value());

        List<LintDefectV2Entity> defectList = lintDefectV2Repository.findDefectsByCheckers(taskId, toolName, excludeStatusSet, checkers);
        if (CollectionUtils.isNotEmpty(defectList))
        {
            long currTime = System.currentTimeMillis();
            List<LintDefectV2Entity> needUpdateDefectList = new ArrayList<>();
            defectList.forEach(defect ->
            {
                String checkerName = defect.getChecker();
                int status = defect.getStatus();

                // 命中关闭的规则
                if (CollectionUtils.isNotEmpty(closedCheckers) && closedCheckers.contains(checkerName))
                {
                    status = status | ComConstants.DefectStatus.CHECKER_MASK.value();
                    if (defect.getExcludeTime() == null || defect.getExcludeTime() == 0)
                    {
                        defect.setExcludeTime(currTime);
                    }
                }
                // 命中打开的规则
                else if (CollectionUtils.isNotEmpty(openCheckers) && openCheckers.contains(checkerName))
                {
                    if ((status & ComConstants.DefectStatus.CHECKER_MASK.value()) > 0)
                    {
                        status = status - ComConstants.DefectStatus.CHECKER_MASK.value();
                        if (status < ComConstants.DefectStatus.PATH_MASK.value())
                        {
                            defect.setExcludeTime(0L);
                        }
                    }
                }

                if (defect.getStatus() != status)
                {
                    defect.setStatus(status);
                    needUpdateDefectList.add(defect);
                }
            });

            lintDefectV2Dao.batchUpdateDefectStatusExcludeBit(taskId, needUpdateDefectList);
        }
        return new Result(CommonMessageCode.SUCCESS);
    }
}

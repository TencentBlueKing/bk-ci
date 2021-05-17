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

package com.tencent.bk.codecc.defect.consumer;

import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.IConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * CCN已关闭告警统计消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component("ccnCloseDefectStatisticConsumer")
@Slf4j
public class CCNCloseDefectStatisticConsumer implements IConsumer<CCNStatisticEntity> {
    @Autowired
    private CCNDefectRepository ccnDefectRepository;
    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;
    @Autowired
    public ThirdPartySystemCaller thirdPartySystemCaller;

    @Override
    public void consumer(CCNStatisticEntity ccnStatisticEntity) {
        Long taskId = ccnStatisticEntity.getTaskId();

        Map<String, String> riskConfigMap = thirdPartySystemCaller.getRiskFactorConfig(ComConstants.Tool.CCN.name());
        if (riskConfigMap == null)
        {
            log.error("Has not init risk factor config!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"风险系数"}, null);
        }
        int sh = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.SH.name()));
        int h = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.H.name()));
        int m = Integer.valueOf(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

        long totalSuperHighFixedCount = 0L;
        long totalHighFixedCount = 0L;
        long totalMiddleFixedCount = 0L;
        long totalLowFixedCount = 0L;
        long totalSuperHighIgnoreCount = 0L;
        long totalHighIgnoreCount = 0L;
        long totalMiddleIgnoreCount = 0L;
        long totalLowIgnoreCount = 0L;
        long totalSuperHighMaskCount = 0L;
        long totalHighMaskCount = 0L;
        long totalMiddleMaskCount = 0L;
        long totalLowMaskCount = 0L;

        // 查询所有已关闭的告警
        List<CCNDefectEntity> allCloseDefectList = ccnDefectRepository.findCloseDefectByTaskId(taskId);
        for (CCNDefectEntity defect : allCloseDefectList) {
            int status = defect.getStatus();
            int ccn = defect.getCcn();
            if (status == (ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.FIXED.value())) {
                if (ccn >= sh) {
                    totalSuperHighFixedCount++;
                } else if (ccn >= h && ccn < sh) {
                    totalHighFixedCount++;
                } else if (ccn >= m && ccn < h) {
                    totalMiddleFixedCount++;
                } else if (ccn < m) {
                    totalLowFixedCount++;
                }
            } else if (status == (ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value())) {
                if (ccn >= sh) {
                    totalSuperHighIgnoreCount++;
                } else if (ccn >= h && ccn < sh) {
                    totalHighIgnoreCount++;
                } else if (ccn >= m && ccn < h) {
                    totalMiddleIgnoreCount++;
                } else if (ccn < m) {
                    totalLowIgnoreCount++;
                }
            } else if (status >=  ComConstants.DefectStatus.PATH_MASK.value()) {
                if (ccn >= sh) {
                    totalSuperHighMaskCount++;
                } else if (ccn >= h && ccn < sh) {
                    totalHighMaskCount++;
                } else if (ccn >= m && ccn < h) {
                    totalMiddleMaskCount++;
                } else if (ccn < m) {
                    totalLowMaskCount++;
                }
            }
        }
        ccnStatisticEntity.setSuperHighFixedCount(totalSuperHighFixedCount);
        ccnStatisticEntity.setHighFixedCount(totalHighFixedCount);
        ccnStatisticEntity.setMiddleFixedCount(totalMiddleFixedCount);
        ccnStatisticEntity.setLowFixedCount(totalLowFixedCount);
        ccnStatisticEntity.setSuperHighIgnoreCount(totalSuperHighIgnoreCount);
        ccnStatisticEntity.setHighIgnoreCount(totalHighIgnoreCount);
        ccnStatisticEntity.setMiddleIgnoreCount(totalMiddleIgnoreCount);
        ccnStatisticEntity.setLowIgnoreCount(totalLowIgnoreCount);
        ccnStatisticEntity.setSuperHighMaskCount(totalSuperHighMaskCount);
        ccnStatisticEntity.setHighMaskCount(totalHighMaskCount);
        ccnStatisticEntity.setMiddleMaskCount(totalMiddleMaskCount);
        ccnStatisticEntity.setLowMaskCount(totalLowMaskCount);

        ccnStatisticRepository.save(ccnStatisticEntity);
    }

}

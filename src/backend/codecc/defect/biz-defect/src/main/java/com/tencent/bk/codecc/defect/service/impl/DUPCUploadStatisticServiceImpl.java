/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.redis.TaskAnalysisDao;
import com.tencent.bk.codecc.defect.model.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.DUPCScanSummaryEntity;
import com.tencent.bk.codecc.defect.model.DUPCStatisticEntity;
import com.tencent.bk.codecc.defect.service.DUPCUploadStatisticService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.DUPCScanSummaryVO;
import com.tencent.bk.codecc.defect.vo.UploadDUPCStatisticVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;

/**
 * @version V1.0
 * @date 2019/6/3
 */
@Service
public class DUPCUploadStatisticServiceImpl implements DUPCUploadStatisticService
{
    private static Logger logger = LoggerFactory.getLogger(DUPCUploadStatisticServiceImpl.class);

    @Autowired
    public TaskAnalysisDao taskAnalysisDao;

    @Autowired
    private DUPCStatisticRepository dupcStatisticRepository;

    @Autowired
    private DUPCDefectRepository dupcDefectRepository;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Override
    public Result uploadStatistic(UploadDUPCStatisticVO uploadStatisticVO)
    {
        // 调用task模块的接口获取任务信息
        Long taskId = uploadStatisticVO.getTaskId();
        String currentAnalysisVersion = taskAnalysisDao.getCurrentAnalysisVersion(taskId, ComConstants.Tool.DUPC.name());

        // 更新告警方法的状态
        int existCount = updateDefectStatus(taskId, currentAnalysisVersion);

        // 保存本次上报文件的告警数据统计数据
        saveStatisticResult(taskId, currentAnalysisVersion, existCount, uploadStatisticVO.getScanSummary());
        return new Result(CommonMessageCode.SUCCESS, "upload DUPC analysis statistic ok");
    }

    /**
     * 保存本次上报文件的告警数据统计数据
     *
     * @param taskId
     * @param currentAnalysisVersion
     * @param existCount
     * @param scanSummary
     */
    private void saveStatisticResult(long taskId, String currentAnalysisVersion, int existCount, DUPCScanSummaryVO scanSummary)
    {
        DUPCStatisticEntity statisticEntity = dupcStatisticRepository.findByAnalysisVersion(currentAnalysisVersion);

        if (statisticEntity == null)
        {
            logger.error("Can't get the analysis[version-{}] result", currentAnalysisVersion);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{currentAnalysisVersion}, null);
        }

        long dupLineCount = scanSummary.getDupLineCount();
        long rawlineCount = scanSummary.getRawlineCount();
        float dupRate = 0.00F;
        if (rawlineCount != 0)
        {
            dupRate = (float) dupLineCount * 100 / rawlineCount;
        }
        statisticEntity.setDefectCount(existCount);
        statisticEntity.setDefectChange(existCount - statisticEntity.getLastDefectCount());
        statisticEntity.setDupRate(dupRate);
        statisticEntity.setDupRateChange(dupRate - statisticEntity.getLastDupRate());
        statisticEntity.setTime(System.currentTimeMillis());
        statisticEntity.setTaskId(taskId);
        statisticEntity.setToolName(ComConstants.Tool.DUPC.name());

        DUPCScanSummaryEntity dupcScanSummary = new DUPCScanSummaryEntity();
        BeanUtils.copyProperties(scanSummary, dupcScanSummary);
        statisticEntity.setDupcScanSummary(dupcScanSummary);
        dupcStatisticRepository.save(statisticEntity);
    }

    private int updateDefectStatus(long taskId, String currentAnalysisVersion)
    {
        int existCount = 0;
        List<DUPCDefectEntity> defectList = dupcDefectRepository.findByTaskIdAndStatus(taskId, DefectConstants.DefectStatus.NEW.value());
        if (CollectionUtils.isNotEmpty(defectList))
        {
            Iterator<DUPCDefectEntity> it = defectList.iterator();
            // 更新告警状态，并统计告警数量
            while (it.hasNext())
            {
                DUPCDefectEntity defectEntity = it.next();
                String analysisVer = defectEntity.getAnalysisVersion();
                logger.info("defect analysis version: {}, current analysis version: {}", analysisVer, currentAnalysisVersion);
                if (StringUtils.isEmpty(analysisVer) || !analysisVer.equalsIgnoreCase(currentAnalysisVersion))
                {
                    long curTime = System.currentTimeMillis();
                    defectEntity.setStatus(null == defectEntity.getStatus() ? 0 : defectEntity.getStatus() | DefectConstants.DefectStatus.FIXED.value());
                    defectEntity.setFixedTime(curTime);
                    defectEntity.setLastUpdateTime(curTime);
                }
                else
                {
                    existCount++;
                    it.remove();
                }
            }
            if (CollectionUtils.isNotEmpty(defectList))
            {
                dupcDefectRepository.save(defectList);
            }
        }
        logger.debug("existCount-->{}", existCount);
        return existCount;
    }
}

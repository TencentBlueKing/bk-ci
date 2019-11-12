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
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.defect.dao.redis.TaskAnalysisDao;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.service.CCNUploadStatisticService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CCNUploadStatisticVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;

/**
 * @version V1.0
 * @date 2019/6/3
 */
@Service
public class CCNUploadStatisticServiceImpl implements CCNUploadStatisticService
{
    private static Logger logger = LoggerFactory.getLogger(CCNUploadStatisticServiceImpl.class);

    @Autowired
    public TaskAnalysisDao taskAnalysisDao;

    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Override
    public Result uploadStatistic(CCNUploadStatisticVO uploadStatisticVO)
    {
        // 调用task模块的接口获取任务信息
        Long taskId = uploadStatisticVO.getTaskId();

        String currentAnalysisVersion = taskAnalysisDao.getCurrentAnalysisVersion(taskId, ComConstants.Tool.CCN.name());

        // 更新告警方法的状态
        int existCount = updateDefectStatus(taskId, currentAnalysisVersion);

        // 保存本次上报文件的告警数据统计数据
        float averageCCN = Float.valueOf(uploadStatisticVO.getAverageCCN());
        saveStatisticResult(taskId, currentAnalysisVersion, existCount, averageCCN);

        return new Result(CommonMessageCode.SUCCESS, "upload CCN analysis statistic ok");
    }

    /**
     * 根据分析版本号判断告警是否已经白修复
     *
     * @param taskId
     * @param currentAnalysisVersion
     * @return
     */
    private int updateDefectStatus(long taskId, String currentAnalysisVersion)
    {
        int existCount = 0;
        List<CCNDefectEntity> ccnDefectList = ccnDefectRepository.findByTaskIdAndStatus(taskId, DefectConstants.DefectStatus.NEW.value());

        if (CollectionUtils.isNotEmpty(ccnDefectList))
        {
            Iterator<CCNDefectEntity> it = ccnDefectList.iterator();
            // 更新告警状态，并统计告警数量
            while (it.hasNext())
            {
                CCNDefectEntity defectEntity = it.next();
                String analysisVer = defectEntity.getAnalysisVersion();
                if (StringUtils.isEmpty(analysisVer) || !analysisVer.equalsIgnoreCase(currentAnalysisVersion))
                {
                    long curTime = System.currentTimeMillis();
                    defectEntity.setStatus(defectEntity.getStatus() | DefectConstants.DefectStatus.FIXED.value());
                    defectEntity.setFixedTime(curTime);
                    defectEntity.setLatestDateTime(curTime);
                }
                else
                {
                    existCount++;
                    it.remove();
                }
            }
            if (CollectionUtils.isNotEmpty(ccnDefectList))
            {
                ccnDefectRepository.save(ccnDefectList);
            }
        }
        logger.info("existCount-->{}", existCount);
        return existCount;
    }

    /**
     * @param taskId
     * @param currentAnalysisVersion
     * @param existCount
     * @param averageCCN
     */
    private void saveStatisticResult(long taskId, String currentAnalysisVersion, int existCount, float averageCCN)
    {
        CCNStatisticEntity ccnStatisticEntity = ccnStatisticRepository.findByAnalysisVersion(currentAnalysisVersion);

        if (ccnStatisticEntity == null)
        {
            logger.error("Can't get the analysis[version-{}] result", currentAnalysisVersion);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{currentAnalysisVersion}, null);
        }

        ccnStatisticEntity.setDefectCount(existCount);
        ccnStatisticEntity.setDefectChange(existCount - ccnStatisticEntity.getLastDefectCount());
        ccnStatisticEntity.setAverageCCN(averageCCN);
        ccnStatisticEntity.setAverageCCNChange(averageCCN - ccnStatisticEntity.getLastAverageCCN());
        ccnStatisticEntity.setTime(System.currentTimeMillis());
        ccnStatisticEntity.setTaskId(taskId);
        ccnStatisticEntity.setToolName(ComConstants.Tool.CCN.name());
        ccnStatisticRepository.save(ccnStatisticEntity);
    }
}

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Lint类工具批量忽略的处理器
 *
 * @version V1.0
 * @date 2020/3/2
 */
@Slf4j
@Service("LINTBatchIgnoreDefectBizService")
public class LintBatchIgnoreDefectBizServiceImpl extends AbstractLintBatchDefectProcessBizService
{
    @Autowired
    private LintDefectV2Dao defectDao;

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        int status = ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value();
        defectList.forEach(defectEntity -> ((LintDefectV2Entity) defectEntity).setStatus(status));
        defectDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defectList, batchDefectProcessReqVO.getIgnoreReasonType(),
                batchDefectProcessReqVO.getIgnoreReason(), batchDefectProcessReqVO.getIgnoreAuthor());

        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }
}

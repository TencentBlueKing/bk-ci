package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 圈复杂度忽略告警实现类
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
@Service("CCNBatchIgnoreDefectBizService")
public class CCNBatchIgnoreDefectBizServiceImpl extends AbstractCCNUpdateDefectStatusService
{
    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        updateDefects(defectList, batchDefectProcessReqVO);
    }

    @Override
    protected void updateDefectStatus(CCNDefectEntity defectEntity, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        int status = ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value();
        defectEntity.setStatus(status);
        defectEntity.setIgnoreTime(System.currentTimeMillis());
        defectEntity.setIgnoreAuthor(batchDefectProcessReqVO.getIgnoreAuthor());
        defectEntity.setIgnoreReason(batchDefectProcessReqVO.getIgnoreReason());
        defectEntity.setIgnoreReasonType(batchDefectProcessReqVO.getIgnoreReasonType());
    }
}

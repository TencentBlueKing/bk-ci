package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 圈复杂度恢复忽略实现类
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
@Service("CCNBatchRevertIgnoreBizService")
public class CCNBatchRevertIgnoreBizServiceImpl extends AbstractCCNUpdateDefectStatusService
{
    /**
     * 获取批处理类型对应的告警状态条件
     * 忽略告警、告警处理人分配、告警标志修改针对的都是待修复告警，而恢复忽略针对的是已忽略告警
     * @return
     */
    @Override
    protected int getStatusCondition()
    {
        return ComConstants.DefectStatus.IGNORE.value();
    }

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        updateDefects(defectList, batchDefectProcessReqVO);
    }

    @Override
    protected void updateDefectStatus(CCNDefectEntity defectEntity, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        if ((defectEntity.getStatus() & ComConstants.DefectStatus.IGNORE.value()) > 0)
        {
            defectEntity.setStatus(defectEntity.getStatus() - ComConstants.DefectStatus.IGNORE.value());
            defectEntity.setIgnoreAuthor(null);
            defectEntity.setIgnoreReason(null);
            defectEntity.setIgnoreReasonType(0);
        }
    }
}

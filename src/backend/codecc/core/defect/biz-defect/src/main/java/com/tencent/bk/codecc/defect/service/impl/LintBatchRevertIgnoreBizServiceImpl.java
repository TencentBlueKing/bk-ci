package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectRepository;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Lint工具批量恢复忽略实现类
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Service("LINTBatchRevertIgnoreBizService")
public class LintBatchRevertIgnoreBizServiceImpl extends AbstractLintUpdateDefectStatusService
{
    @Autowired
    private LintDefectRepository lintDefectRepository;
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
        updateFileDefectStatus(defectList, batchDefectProcessReqVO);
    }

    @Override
    protected void updateDefectStatus(LintDefectEntity defectEntity, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        if ((defectEntity.getStatus() & ComConstants.DefectStatus.IGNORE.value()) > 0)
        {
            defectEntity.setStatus(defectEntity.getStatus() - ComConstants.DefectStatus.IGNORE.value());
            defectEntity.setIgnoreTime(System.currentTimeMillis());
            defectEntity.setIgnoreAuthor(null);
            defectEntity.setIgnoreReason(null);
            defectEntity.setIgnoreReasonType(0);
        }
    }
}

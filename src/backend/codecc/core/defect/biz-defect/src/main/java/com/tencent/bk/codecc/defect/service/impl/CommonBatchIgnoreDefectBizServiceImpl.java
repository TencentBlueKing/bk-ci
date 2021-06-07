package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 批量忽略的处理器
 *
 * @version V1.0
 * @date 2019/10/31
 */
@Slf4j
@Service("CommonBatchIgnoreDefectBizService")
public class CommonBatchIgnoreDefectBizServiceImpl extends AbstractCommonBatchDefectProcessBizService
{
    @Autowired
    private DefectDao defectDao;

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        int status = ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value();
        defectList.forEach(defectEntity -> ((DefectEntity)defectEntity).setStatus(status));
        defectDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defectList, batchDefectProcessReqVO.getIgnoreReasonType(),
                batchDefectProcessReqVO.getIgnoreReason(), batchDefectProcessReqVO.getIgnoreAuthor());

        // 2.异步批量更新tapd告警状态
//        asynBatchUpdateTapdDefects(taskId, defectKeySet);

        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }

    /**
     * 异步批量更新tapd告警状态
     *
     * @param projId
     * @param defectKeySet
     */
    private void asynBatchUpdateTapdDefects(long projId, Set<String> defectKeySet)
    {
    }
}

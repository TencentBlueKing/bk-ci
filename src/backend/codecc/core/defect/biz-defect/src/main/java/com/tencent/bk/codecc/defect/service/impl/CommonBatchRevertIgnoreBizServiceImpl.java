package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.devops.common.constant.ComConstants;
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
@Service("CommonBatchRevertIgnoreBizService")
public class CommonBatchRevertIgnoreBizServiceImpl extends AbstractCommonBatchDefectProcessBizService
{
    @Autowired
    private DefectDao defectDao;

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
        defectList.forEach(defectEntity -> ((DefectEntity)defectEntity).setStatus(((DefectEntity)defectEntity).getStatus() - ComConstants.DefectStatus.IGNORE.value()));
        defectDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defectList, 0, null, null);

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

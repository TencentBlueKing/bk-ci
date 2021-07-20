package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.devops.common.constant.ComConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lint工具批量恢复忽略实现类
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Service("LINTBatchRevertIgnoreBizService")
public class LintBatchRevertIgnoreBizServiceImpl extends AbstractLintBatchDefectProcessBizService
{
    @Autowired
    private LintDefectV2Dao defectDao;

    /**
     * 获取批处理类型对应的告警状态条件
     * 忽略告警、告警处理人分配、告警标志修改针对的都是待修复告警，而恢复忽略针对的是已忽略告警
     *
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
        List<LintDefectV2Entity> defects = ((List<LintDefectV2Entity>) defectList).stream()
                .filter(it -> (it.getStatus() & ComConstants.DefectStatus.IGNORE.value()) > 0)
                .map(it ->
                {
                    it.setStatus(it.getStatus() - ComConstants.DefectStatus.IGNORE.value());
                    return it;
                }).collect(Collectors.toList());

        defectDao.batchUpdateDefectStatusIgnoreBit(batchDefectProcessReqVO.getTaskId(), defects, 0, null, null);

        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }
}

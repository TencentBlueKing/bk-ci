package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Lint类工具批量标识的处理器
 *
 * @version V1.0
 * @date 2020/3/2
 */
@Slf4j
@Service("LINTBatchMarkDefectBizService")
public class LintBatchMarkDefectBizServiceImpl extends AbstractLintBatchDefectProcessBizService
{
    @Autowired
    private LintDefectV2Dao defectDao;

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        defectDao.batchMarkDefect(batchDefectProcessReqVO.getTaskId(), defectList, batchDefectProcessReqVO.getMarkFlag());
    }

}

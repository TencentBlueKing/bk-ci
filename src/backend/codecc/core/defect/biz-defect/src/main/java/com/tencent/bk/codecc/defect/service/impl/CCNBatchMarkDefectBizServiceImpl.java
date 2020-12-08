package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 批量忽略的处理器
 *
 * @version V1.0
 * @date 2019/10/31
 */
@Service("CCNBatchMarkDefectBizService")
public class CCNBatchMarkDefectBizServiceImpl extends AbstractCCNBatchDefectProcessBizService
{
    @Autowired
    private CCNDefectDao defectDao;

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        defectDao.batchMarkDefect(batchDefectProcessReqVO.getTaskId(), defectList, batchDefectProcessReqVO.getMarkFlag());
    }
}

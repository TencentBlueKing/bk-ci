package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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
@Service("CommonBatchAssignDefectBizService")
@Slf4j
public class CommonBatchAssignDefectBizServiceImpl extends AbstractCommonBatchDefectProcessBizService
{
    @Autowired
    private DefectDao defectDao;

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        Set<String> newAuthor = batchDefectProcessReqVO.getNewAuthor();
        if (CollectionUtils.isEmpty(newAuthor))
        {
            log.error("parameter [newAuthor] can't be empty");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{newAuthor.toString()}, null);
        }
        defectDao.batchUpdateDefectAuthor(batchDefectProcessReqVO.getTaskId(), defectList, newAuthor);

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

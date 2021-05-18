package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.service.AbstractBatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * 圈复杂度告警处理处理抽象类
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
public abstract class AbstractCCNBatchDefectProcessBizService extends AbstractBatchDefectProcessBizService
{
    @Autowired
    private CCNDefectDao ccnDefectDao;

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Override
    protected abstract void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO);

    @Override
    protected List getDefectsByQueryCond(long taskId, DefectQueryReqVO defectQueryReqVO)
    {
        // 根据任务ID和工具名查询所有的告警
        CCNDefectQueryRspVO defectQueryRspVO = new CCNDefectQueryRspVO();

        Set<String> fileList = defectQueryReqVO.getFileList();
        String author = defectQueryReqVO.getAuthor();
        List<CCNDefectEntity> defectList = ccnDefectDao.findByTaskIdAndAuthorAndRelPaths(taskId, author, fileList);

        IQueryWarningBizService queryWarningBizService = factory.createBizService(defectQueryReqVO.getToolName(),
                ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        queryWarningBizService.filterDefectByCondition(taskId, defectList, null, defectQueryReqVO, defectQueryRspVO, null);

        return defectList;
    }

    /**
     * 根据前端传入的告警key，查询有效的告警
     * 过滤规则是：忽略告警、告警处理人分配、告警标志修改针对的都是待修复告警，而恢复忽略针对的是已忽略告警
     * @param batchDefectProcessReqVO
     */
    @Override
    protected List<CCNDefectEntity> getEffectiveDefectByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        List<CCNDefectEntity> defecEntityList = ccnDefectRepository.findByEntityIdIn(batchDefectProcessReqVO.getDefectKeySet());
        if (CollectionUtils.isEmpty(defecEntityList))
        {
            return new ArrayList<>();
        }

        Iterator<CCNDefectEntity> it = defecEntityList.iterator();
        while (it.hasNext())
        {
            CCNDefectEntity defectEntity = it.next();
            int status = defectEntity.getStatus();
            int statusCond = getStatusCondition();
            boolean notMatchNewStatus = statusCond == ComConstants.DefectStatus.NEW.value() && status != ComConstants.DefectStatus.NEW.value();
            boolean notMatchIgnoreStatus = statusCond > ComConstants.DefectStatus.NEW.value() && (status & statusCond) == 0;
            if (notMatchNewStatus || notMatchIgnoreStatus)
            {
                it.remove();
            }
        }
        return defecEntityList;
    }
}

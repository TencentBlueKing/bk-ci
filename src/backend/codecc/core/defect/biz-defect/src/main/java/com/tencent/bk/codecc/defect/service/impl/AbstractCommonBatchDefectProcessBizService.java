package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.service.AbstractBatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.DefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
public abstract class AbstractCommonBatchDefectProcessBizService extends AbstractBatchDefectProcessBizService
{
    @Autowired
    private DefectRepository defectRepository;

    /**
     * 根据前端传入的条件查询告警键值
     *
     *
     * @param taskId
     * @param defectQueryReqVO
     * @return
     */
    @Override
    protected List getDefectsByQueryCond(long taskId, DefectQueryReqVO defectQueryReqVO)
    {
        // 根据任务ID和工具名查询所有的告警
        DefectQueryRspVO defectQueryRspVO = new DefectQueryRspVO();
        List<DefectEntity> defectList = defectRepository.findByTaskIdAndToolName(taskId, defectQueryReqVO.getToolName());
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
    protected List<DefectEntity> getEffectiveDefectByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        List<DefectEntity> defecEntityList = defectRepository.findStatusByEntityIdIn(batchDefectProcessReqVO.getDefectKeySet());
        if (CollectionUtils.isEmpty(defecEntityList))
        {
            return new ArrayList<>();
        }

        Iterator<DefectEntity> it = defecEntityList.iterator();
        while (it.hasNext())
        {
            DefectEntity defectEntity = it.next();
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

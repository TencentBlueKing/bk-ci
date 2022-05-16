package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

/**
 * 圈复杂度更新告警状态抽象类
 *
 * @version V1.0
 * @date 2020/3/4
 */
@Slf4j
public abstract class AbstractCCNUpdateDefectStatusService extends AbstractCCNBatchDefectProcessBizService
{
    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    protected abstract void updateDefectStatus(CCNDefectEntity defectEntity, BatchDefectProcessReqVO batchDefectProcessReqVO);

    protected void updateDefects(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        Set<String> defectEntityIds = Sets.newHashSet();
        if (CollectionUtils.isNotEmpty(defectList))
        {
            for (Object defectObj : defectList)
            {
                CCNDefectEntity defectVO = (CCNDefectEntity)defectObj;
                defectEntityIds.add(defectVO.getEntityId());
            }
        }
        List<CCNDefectEntity> defectEntities = ccnDefectRepository.findByEntityIdIn(defectEntityIds);
        if (CollectionUtils.isNotEmpty(defectEntities))
        {
            for (CCNDefectEntity defectEntity : defectEntities)
            {
                updateDefectStatus(defectEntity, batchDefectProcessReqVO);
            }
        }
        ccnDefectRepository.saveAll(defectEntities);
        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }
}

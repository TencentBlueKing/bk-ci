package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectRepository;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lint类工具告警状态更新抽象类
 *
 * @version V1.0
 * @date 2020/3/4
 */
public abstract class AbstractLintUpdateDefectStatusService extends AbstractLintBatchDefectProcessBizService
{
    @Autowired
    private LintDefectRepository lintDefectRepository;

    protected abstract void updateDefectStatus(LintDefectEntity defectEntity, BatchDefectProcessReqVO batchDefectProcessReqVO);

    protected void updateFileDefectStatus(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        Map<String, Set<String>> fileDefectMap = getFileDefectMap(defectList);
        List<LintFileEntity> updateFileEntities = Lists.newArrayList();
        Set<String> fileEntityIds = Sets.newHashSet();
        List<LintFileEntity> fileEntities = lintDefectRepository.findByEntityIdIn(fileDefectMap.keySet());
        if (CollectionUtils.isNotEmpty(fileEntities))
        {
            for (LintFileEntity fileEntity : fileEntities)
            {
                if (CollectionUtils.isNotEmpty(fileEntity.getDefectList()))
                {
                    Set<String> fileDefectIds = fileDefectMap.get(fileEntity.getEntityId());
                    for (LintDefectEntity defectEntity : fileEntity.getDefectList())
                    {
                        if (fileDefectIds != null && fileDefectIds.contains(defectEntity.getDefectId()))
                        {
                            updateDefectStatus(defectEntity, batchDefectProcessReqVO);
                            if (fileEntityIds.add(fileEntity.getEntityId()))
                            {
                                updateFileEntities.add(fileEntity);
                            }
                        }
                    }
                }
            }
        }

        if (CollectionUtils.isNotEmpty(updateFileEntities))
        {
            lintDefectRepository.save(updateFileEntities);
        }
    }
}

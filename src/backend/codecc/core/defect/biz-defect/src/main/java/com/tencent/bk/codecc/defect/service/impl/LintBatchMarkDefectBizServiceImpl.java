package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectRepository;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.service.AbstractBatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.LintDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.LintDefectVO;
import com.tencent.bk.codecc.defect.vo.QueryFileDefectVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private LintDefectRepository lintDefectRepository;

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        Map<String, Set<String>> fileDefectMap = getFileDefectMap(defectList);

        long currTime = System.currentTimeMillis();
        List<LintFileEntity> updateFileEntities = Lists.newArrayList();
        List<LintFileEntity> fileEntities = lintDefectRepository.findByEntityIdIn(fileDefectMap.keySet());
        if (CollectionUtils.isNotEmpty(fileEntities))
        {
            for (LintFileEntity fileEntity : fileEntities)
            {
                Set<String> fileDefectIds = fileDefectMap.get(fileEntity.getEntityId());
                boolean needupdate = false;
                if (CollectionUtils.isNotEmpty(fileEntity.getDefectList()) && CollectionUtils.isNotEmpty(fileDefectIds))
                {
                    for (LintDefectEntity defectEntity : fileEntity.getDefectList())
                    {
                        if (fileDefectIds.contains(defectEntity.getDefectId()))
                        {
                            defectEntity.setMark(batchDefectProcessReqVO.getMarkFlag());
                            defectEntity.setMarkTime(currTime);
                            needupdate = true;
                        }
                    }
                }
                if (needupdate)
                {
                    updateFileEntities.add(fileEntity);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(updateFileEntities))
        {
            lintDefectRepository.save(updateFileEntities);
        }
    }

}

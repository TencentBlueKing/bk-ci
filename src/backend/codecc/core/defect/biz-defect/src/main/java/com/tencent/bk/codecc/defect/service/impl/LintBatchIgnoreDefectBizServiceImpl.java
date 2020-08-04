package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectRepository;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lint类工具批量忽略的处理器
 *
 * @version V1.0
 * @date 2020/3/2
 */
@Slf4j
@Service("LINTBatchIgnoreDefectBizService")
public class LintBatchIgnoreDefectBizServiceImpl extends AbstractLintUpdateDefectStatusService
{
    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        updateFileDefectStatus(defectList, batchDefectProcessReqVO);
    }

    @Override
    protected void updateDefectStatus(LintDefectEntity defectEntity, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        int status = ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value();
        defectEntity.setStatus(status);
        defectEntity.setIgnoreTime(System.currentTimeMillis());
        defectEntity.setIgnoreAuthor(batchDefectProcessReqVO.getIgnoreAuthor());
        defectEntity.setIgnoreReason(batchDefectProcessReqVO.getIgnoreReason());
        defectEntity.setIgnoreReasonType(batchDefectProcessReqVO.getIgnoreReasonType());
    }
}

package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.List2StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Lint类工具批量标识的处理器
 *
 * @version V1.0
 * @date 2020/3/2
 */
@Slf4j
@Service("LINTBatchAssignDefectBizService")
public class LintBatchAssignDefectBizServiceImpl extends AbstractLintBatchDefectProcessBizService
{
    @Autowired
    private LintDefectV2Dao defectDao;

    @Override
    protected void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        Set<String> newAuthor = batchDefectProcessReqVO.getNewAuthor();
        if (CollectionUtils.isEmpty(newAuthor))
        {
            log.error("parameter [newAuthor] can't be empty");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{newAuthor.toString()}, null);
        }

        String newAuthorStr = List2StrUtil.toString(newAuthor, ComConstants.SEMICOLON);

        defectDao.batchUpdateDefectAuthor(batchDefectProcessReqVO.getTaskId(), defectList, newAuthorStr);

        refreshOverviewData(batchDefectProcessReqVO.getTaskId());
    }

}

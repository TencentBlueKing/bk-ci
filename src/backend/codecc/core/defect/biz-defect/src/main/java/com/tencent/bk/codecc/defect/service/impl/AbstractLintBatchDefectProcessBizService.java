package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectDao;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.service.AbstractBatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.vo.*;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Lint类工具告警批量处理抽象类
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
public abstract class AbstractLintBatchDefectProcessBizService extends AbstractBatchDefectProcessBizService
{
    @Autowired
    private LintDefectRepository lintDefectRepository;

    @Autowired
    private LintDefectDao lintDefectDao;

    @Autowired
    private BizServiceFactory<IQueryWarningBizService> fileAndDefectQueryFactory;

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
        String toolName = defectQueryReqVO.getToolName();

        LintDefectQueryRspVO lintFileQueryRsp = new LintDefectQueryRspVO();

        Set<String> fileList = defectQueryReqVO.getFileList();
        String checker = defectQueryReqVO.getChecker();
        String author = defectQueryReqVO.getAuthor();

        // 根据任务ID和工具名查询所有的告警
        List<LintFileEntity> originalFileInfoEntityList = lintDefectDao.findFileListByParams(taskId, toolName, fileList, checker, author);

        // 按过滤条件过滤告警
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(toolName,
                ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        queryWarningBizService.filterDefectByCondition(taskId, originalFileInfoEntityList, defectQueryReqVO, lintFileQueryRsp);

        return originalFileInfoEntityList;
    }

    @Override
    protected List getEffectiveDefectByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        List<QueryFileDefectVO> fileDefectVOS = batchDefectProcessReqVO.getFileDefects();
        Map<String, Set<String>> fileDefectMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(fileDefectVOS))
        {
            for (QueryFileDefectVO fileDefectVO : fileDefectVOS)
            {
                if (fileDefectMap.get(fileDefectVO.getFileEntityId()) == null)
                {
                    fileDefectMap.put(fileDefectVO.getFileEntityId(), Sets.newHashSet());
                }

                if (StringUtils.isNotEmpty(fileDefectVO.getDefectId()))
                {
                    fileDefectMap.get(fileDefectVO.getFileEntityId()).add(fileDefectVO.getDefectId());
                }
            }
        }
        List<LintFileEntity> fileEntities = lintDefectRepository.findByEntityIdIn(fileDefectMap.keySet());
        if (CollectionUtils.isNotEmpty(fileEntities))
        {
            Iterator<LintFileEntity> it = fileEntities.iterator();
            while (it.hasNext())
            {
                LintFileEntity fileEntity = it.next();
                if (CollectionUtils.isNotEmpty(fileEntity.getDefectList()))
                {
                    Set<String> fileDefectIds = fileDefectMap.get(fileEntity.getEntityId());

                    if (CollectionUtils.isNotEmpty(fileDefectIds))
                    {
                        Iterator<LintDefectEntity> defectEntityIt = fileEntity.getDefectList().iterator();
                        while (defectEntityIt.hasNext())
                        {
                            LintDefectEntity defectEntity = defectEntityIt.next();
                            if (!fileDefectIds.contains(defectEntity.getDefectId()))
                            {
                                defectEntityIt.remove();
                            }
                        }
                        if (fileEntity.getDefectList().size() == 0)
                        {
                            it.remove();
                        }
                    }
                }
            }
        }
        return fileEntities;
    }

    protected Map<String, Set<String>> getFileDefectMap(List defectList)
    {
        Map<String, Set<String>> fileDefectMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(defectList))
        {
            for (Object fileDefectObj : defectList)
            {
                LintFileEntity lintFileEntity = (LintFileEntity)fileDefectObj;
                if (fileDefectMap.get(lintFileEntity.getEntityId()) == null)
                {
                    fileDefectMap.put(lintFileEntity.getEntityId(), Sets.newHashSet());
                }
                if (CollectionUtils.isNotEmpty(lintFileEntity.getDefectList()))
                {
                    for (LintDefectEntity lintDefectEntity : lintFileEntity.getDefectList())
                    {
                        fileDefectMap.get(lintFileEntity.getEntityId()).add(lintDefectEntity.getDefectId());
                    }
                }
            }
        }
        return fileDefectMap;
    }
}

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.codecc.defect.component.LintDefectCache;
import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.dao.mongorepository.FirstAnalysisSuccessTimeRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.TransferAuthorRepository;
import com.tencent.bk.codecc.defect.dao.redis.TaskAnalysisDao;
import com.tencent.bk.codecc.defect.model.FirstAnalysisSuccessEntity;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.bk.codecc.defect.service.AbstractUploadDefectBizService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.JsonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 任务分析记录服务层实现
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service("LINTUploadDefectBizService")
public class LintUploadDefectBizServiceImpl extends AbstractUploadDefectBizService
{
    private static Logger logger = LoggerFactory.getLogger(LintUploadDefectBizServiceImpl.class);

    @Autowired
    private LintDefectRepository lintDefectRepository;

    @Autowired
    private TransferAuthorRepository transferAuthorRepository;

    @Autowired
    private FirstAnalysisSuccessTimeRepository firstSuccessTimeRepository;

    @Autowired
    private TaskAnalysisDao taskAnalysisDao;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Autowired
    private LintDefectCache lintDefectCache;

    /**
     * 移除非法（平台没录入）规则的告警
     *
     * @param defectList
     */
    private void removeInvalidCheckerDefect(List<LintDefectEntity> defectList, String toolName)
    {
        if (CollectionUtils.isNotEmpty(defectList))
        {
            Iterator<LintDefectEntity> it = defectList.iterator();
            while (it.hasNext())
            {
                LintDefectEntity defect = it.next();
                if (0 == lintDefectCache.getDefectLevel(toolName, defect.getChecker()))
                {
                    logger.error("Checker invalid! checker: {}", defect.getChecker());
                    it.remove();
                }
            }
        }

    }

    @Override
    public Result processBiz(UploadDefectVO uploadDefectVO)
    {
        String defectListJson = decompressDefects(uploadDefectVO.getDefectsCompress());
        List<LintDefectEntity> defectList = JsonUtil.INSTANCE.to(defectListJson, new TypeReference<List<LintDefectEntity>>()
        {
        });

        String toolName = uploadDefectVO.getToolName();
        // 移除非法（平台没录入）规则的告警
        removeInvalidCheckerDefect(defectList, toolName);
        if (CollectionUtils.isEmpty(defectList))
        {
            logger.error("File does not contain valid checker!");
            return new Result(CommonMessageCode.SUCCESS, "File does not contain valid checker!");
        }

        // 调用task模块的接口获取任务信息
        long taskId = uploadDefectVO.getTaskId();

        LintFileEntity fileEntity = lintDefectRepository.findFirstByTaskIdAndToolNameAndRelPath(taskId, toolName, uploadDefectVO.getRelPath());

        //新上报文件
        if (fileEntity == null)
        {
            logger.info("file does not exist, will create it");
            procNewFile(uploadDefectVO, defectList);
        }
        //当前数据库已经存在该文件
        else
        {
            logger.info("file exists, will mongotemplate it");
            procExistFile(fileEntity, uploadDefectVO, defectList);
        }

        return new Result(CommonMessageCode.SUCCESS, "upload defect ok");
    }

    /**
     * 处理新上报告警文件
     *
     * @param uploadDefectVO
     * @param defectList
     */
    private void procNewFile(UploadDefectVO uploadDefectVO, List<LintDefectEntity> defectList)
    {
        LintFileEntity fileEntity = new LintFileEntity();
        BeanUtils.copyProperties(uploadDefectVO, fileEntity);

        // 相对路径relPath的MD5是文件的唯一标志
        long currentTime = System.currentTimeMillis();
        fileEntity.setCreateTime(currentTime);
        fileEntity.setCreatedDate(currentTime);
        fileEntity.setUpdatedDate(currentTime);
        processFileDefect(fileEntity, uploadDefectVO, defectList);
    }

    /**
     * 处理已经存在的告警文件
     *
     * @param fileEntity
     * @param uploadDefectVO
     * @param defectList
     */
    private void procExistFile(LintFileEntity fileEntity, UploadDefectVO uploadDefectVO, List<LintDefectEntity> defectList)
    {
        LintFileEntity newFileEntity = new LintFileEntity();
        BeanUtils.copyProperties(uploadDefectVO, newFileEntity);
        newFileEntity.setEntityId(fileEntity.getEntityId());
        newFileEntity.setCreateTime(fileEntity.getCreateTime());
        newFileEntity.setFixedTime(fileEntity.getFixedTime());
        newFileEntity.setExcludeTime(fileEntity.getExcludeTime());
        processFileDefect(newFileEntity, uploadDefectVO, defectList);
    }

    /**
     * 告警处理逻辑的说明：
     * 工具侧每次上报告警，被路径屏蔽的和和被规则屏蔽的都不扫描上报，因此，上报上来的告警状态肯定都是new
     *
     * @param fileEntity
     * @param uploadDefectVO
     * @param defectList
     */
    private void processFileDefect(LintFileEntity fileEntity, UploadDefectVO uploadDefectVO, List<LintDefectEntity> defectList)
    {
        long taskId = uploadDefectVO.getTaskId();
        String toolName = uploadDefectVO.getToolName();
        TransferAuthorEntity transferAuthorEntity = transferAuthorRepository.findFirstByTaskIdAndToolName(taskId, toolName);
        List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList = null;
        if (transferAuthorEntity != null)
        {
            transferAuthorList = transferAuthorEntity.getTransferAuthorList();
        }
        FirstAnalysisSuccessEntity firstSuccessTimeEntity = firstSuccessTimeRepository.findFirstByTaskIdAndToolName(taskId, toolName);
        long firstSuccessTime = 0L;
        if (firstSuccessTimeEntity != null)
        {
            firstSuccessTime = firstSuccessTimeEntity.getFirstAnalysisSuccessTime();
        }

        int newCount = 0;
        int historyCount = 0;
        Set<String> checkerSet = new HashSet<>();
        Set<String> authorSet = new HashSet<>();
        for (LintDefectEntity lintDefectEntity : defectList)
        {
            // 1.判断是新告警还是历史告警
            long lineUpdateTime = lintDefectEntity.getLineUpdateTime();
            if (firstSuccessTime != 0 && firstSuccessTime < lineUpdateTime)
            {
                lintDefectEntity.setDefectType(DefectConstants.DefectType.NEW.value());
                newCount++;
            }
            else
            {
                lintDefectEntity.setDefectType(DefectConstants.DefectType.HISTORY.value());
                historyCount++;
            }

            // 2.每次上报上来的告警状态肯定都是new的，因为被路径屏蔽或者被规则屏蔽的告警都不会上传
            lintDefectEntity.setStatus(DefectConstants.DefectStatus.NEW.value());

            // 告警严重程度
            lintDefectEntity.setSeverity(DefectConstants.DefectSeverity.NORMAL.value());

            // 作者转换
            if (CollectionUtils.isNotEmpty(transferAuthorList))
            {
                for (TransferAuthorEntity.TransferAuthorPair trasferAuthorPair : transferAuthorList)
                {
                    String author = lintDefectEntity.getAuthor();
                    if (StringUtils.isNotEmpty(author) && author.equalsIgnoreCase(trasferAuthorPair.getSourceAuthor()))
                    {
                        lintDefectEntity.setAuthor(trasferAuthorPair.getTargetAuthor());
                    }
                }
            }

            authorSet.add(lintDefectEntity.getAuthor());
            checkerSet.add(lintDefectEntity.getChecker());
        }

        fileEntity.setStatus(DefectConstants.DefectStatus.NEW.value());
        fileEntity.setAuthorList(authorSet);
        fileEntity.setCheckerList(checkerSet);
        fileEntity.setDefectList(defectList);
        fileEntity.setDefectCount(defectList.size());
        fileEntity.setNewCount(newCount);
        fileEntity.setHistoryCount(historyCount);

        String analysisVersion = taskAnalysisDao.getCurrentAnalysisVersion(taskId, toolName);
        fileEntity.setAnalysisVersion(analysisVersion);
        lintDefectRepository.save(fileEntity);
    }
}

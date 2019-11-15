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
import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.dao.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.TransferAuthorRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.dao.redis.TaskAnalysisDao;
import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.bk.codecc.defect.service.AbstractUploadDefectBizService;
import com.tencent.bk.codecc.defect.utils.FunctionSignatureBuilder;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.JsonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务分析记录服务层实现
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service("CCNUploadDefectBizService")
public class CCNUploadDefectBizServiceImpl extends AbstractUploadDefectBizService
{
    private static Logger logger = LoggerFactory.getLogger(CCNUploadDefectBizServiceImpl.class);

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Autowired
    private TransferAuthorRepository transferAuthorRepository;

    @Autowired
    private TaskAnalysisDao taskAnalysisDao;

    @Autowired
    private CCNDefectDao ccnDefectDao;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Override
    public Result processBiz(UploadDefectVO uploadDefectVO)
    {
        String toolName = uploadDefectVO.getToolName();
        String relPath = uploadDefectVO.getRelPath();
        String defectListJson = decompressDefects(uploadDefectVO.getDefectsCompress());
        List<CCNDefectEntity> defectList = JsonUtil.INSTANCE.to(defectListJson, new TypeReference<List<CCNDefectEntity>>()
        {
        });
        uploadDefectVO.setDefectsCompress(null);

        if (CollectionUtils.isEmpty(defectList))
        {
            logger.error("File does not contain ccn defect.");
            return new Result(CommonMessageCode.SUCCESS, "File does not contain ccn defect.");
        }

        long taskId = uploadDefectVO.getTaskId();

        // 查询已存在告警
        List<CCNDefectEntity> defectEntityList = ccnDefectRepository.findByTaskIdAndRelPath(taskId, relPath);
        Map<String, CCNDefectEntity> defectEntityMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(defectEntityList))
        {
            for (CCNDefectEntity defect : defectEntityList)
            {
                defectEntityMap.put(defect.getFuncSignature(), defect);
            }
        }

        String currentAnalysisVersion = taskAnalysisDao.getCurrentAnalysisVersion(taskId, toolName);
        TransferAuthorEntity transferAuthorEntity = transferAuthorRepository.findFirstByTaskIdAndToolName(taskId, toolName);
        List<TransferAuthorEntity.TransferAuthorPair> transferAuthorList = null;
        if (transferAuthorEntity != null)
        {
            transferAuthorList = transferAuthorEntity.getTransferAuthorList();
        }

        for (CCNDefectEntity defectEntity : defectList)
        {
            // 生成方法的唯一签名
            String funcSignature = FunctionSignatureBuilder.generateFuncSignature(defectEntity.getLongName(), uploadDefectVO.getRelPath());
            defectEntity.setFuncSignature(funcSignature);
            CCNDefectEntity oldDefectEntity = defectEntityMap.get(funcSignature);
            // 新增的告警方法
            if (oldDefectEntity == null)
            {
                defectEntity.setStatus(DefectConstants.DefectStatus.NEW.value());
                defectEntity.setCreateTime(System.currentTimeMillis());
            }
            else
            {
                if ((oldDefectEntity.getStatus() & DefectConstants.DefectStatus.FIXED.value()) > 0)
                {
                    defectEntity.setStatus(oldDefectEntity.getStatus() - DefectConstants.DefectStatus.FIXED.value());
                }
                else
                {
                    defectEntity.setStatus(oldDefectEntity.getStatus());
                }
                defectEntity.setCreateTime(oldDefectEntity.getCreateTime());
                defectEntity.setFixedTime(oldDefectEntity.getFixedTime());
                defectEntity.setIgnoreTime(oldDefectEntity.getIgnoreTime());
                defectEntity.setExcludeTime(oldDefectEntity.getExcludeTime());
            }

            defectEntity.setAnalysisVersion(currentAnalysisVersion);
            defectEntity.setTaskId(taskId);
            defectEntity.setFilePath(uploadDefectVO.getFilePath());
            defectEntity.setUrl(uploadDefectVO.getUrl());
            defectEntity.setBranch(uploadDefectVO.getBranch());
            defectEntity.setRepoId(uploadDefectVO.getRepoId());
            defectEntity.setRevision(uploadDefectVO.getRevision());
            defectEntity.setRelPath(relPath);
            defectEntity.setSubModule(uploadDefectVO.getSubModule());

            // 根据处理人转换关系转换告警处理人
            if (CollectionUtils.isNotEmpty(transferAuthorList))
            {
                for (TransferAuthorEntity.TransferAuthorPair authorTransModel : transferAuthorList)
                {
                    String author = defectEntity.getAuthor();
                    if (StringUtils.isNotEmpty(author) && author.equalsIgnoreCase(authorTransModel.getSourceAuthor()))
                    {
                        defectEntity.setAuthor(authorTransModel.getTargetAuthor());
                    }
                }
            }
        }

        // 批量保存告警列表
        ccnDefectDao.upsertCCNDefectListBySignature(defectList);

        return new Result(CommonMessageCode.SUCCESS, "upload defect ok");
    }
}

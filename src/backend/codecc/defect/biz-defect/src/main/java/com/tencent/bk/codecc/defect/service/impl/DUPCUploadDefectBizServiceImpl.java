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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.defect.component.RiskConfigCache;
import com.tencent.bk.codecc.defect.constant.DefectConstants;
import com.tencent.bk.codecc.defect.dao.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.defect.dao.redis.TaskAnalysisDao;
import com.tencent.bk.codecc.defect.model.CodeBlockEntity;
import com.tencent.bk.codecc.defect.model.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.service.AbstractUploadDefectBizService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.JsonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 任务分析记录服务层实现
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service("DUPCUploadDefectBizService")
public class DUPCUploadDefectBizServiceImpl extends AbstractUploadDefectBizService
{
    private static Logger logger = LoggerFactory.getLogger(DUPCUploadDefectBizServiceImpl.class);

    @Autowired
    private TaskAnalysisDao taskAnalysisDao;
    @Autowired
    private DUPCDefectRepository dupcDefectRepository;
    @Autowired
    private RiskConfigCache riskConfigCache;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Result processBiz(UploadDefectVO uploadDefectVO)
    {
        String defectListJson = decompressDefects(uploadDefectVO.getDefectsCompress());
        DUPCDefectEntity dupcDefectEntity = JsonUtil.INSTANCE.to(defectListJson, new TypeReference<DUPCDefectEntity>()
        {
        });
        uploadDefectVO.setDefectsCompress(null);

        if (dupcDefectEntity == null)
        {
            logger.error("File does not contain dupc defect.");
            return new Result(CommonMessageCode.SUCCESS, "File does not contain dupc defect.");
        }

        // 调用task模块的接口获取任务信息
        long taskId = uploadDefectVO.getTaskId();
        dupcDefectEntity.setTaskId(taskId);

        // 1.处理重复代码块信息
        handleDupBlock(dupcDefectEntity);

        // 2、处理文件信息
        handDefectFileInfo(dupcDefectEntity, uploadDefectVO);


        return new Result(CommonMessageCode.SUCCESS, "upload defect ok");
    }

    /**
     * 处理文件信息
     *
     * @param dupcDefectEntity
     */
    private void handDefectFileInfo(DUPCDefectEntity dupcDefectEntity, UploadDefectVO uploadDefectVO)
    {
        dupcDefectEntity.setToolName(uploadDefectVO.getToolName());
//        dupcDefectEntity.setFilePath(uploadDefectVO.getFilePath());
        dupcDefectEntity.setUrl(uploadDefectVO.getUrl());
        dupcDefectEntity.setBranch(uploadDefectVO.getBranch());
        dupcDefectEntity.setRepoId(uploadDefectVO.getRepoId());
        dupcDefectEntity.setRevision(uploadDefectVO.getRevision());
        dupcDefectEntity.setRelPath(uploadDefectVO.getRelPath());
        dupcDefectEntity.setSubModule(uploadDefectVO.getSubModule());


        String currentAnalysisVersion = taskAnalysisDao.getCurrentAnalysisVersion(dupcDefectEntity.getTaskId(), dupcDefectEntity.getToolName());
        dupcDefectEntity.setAnalysisVersion(currentAnalysisVersion);

//        String entityId = MD5Utils.getMD5(dupcDefectEntity.getRelPath());
//        dupcDefectEntity.setEntityId(entityId);

        //获取风险系数值
        Map<String, String> riskConfigMap = riskConfigCache.getRiskConfig(ComConstants.Tool.DUPC.name());
        float m = Float.valueOf(riskConfigMap.get(ComConstants.RiskFactor.M.name()));

        // 转换文件重复率
        String dupRateStr = dupcDefectEntity.getDupRate();
        float dupRate = Float.valueOf(StringUtils.isEmpty(dupRateStr) ? "0" : dupRateStr.substring(0, dupRateStr.length() - 1));
        dupcDefectEntity.setDupRateValue(dupRate);

        long curTime = System.currentTimeMillis();
        DUPCDefectEntity oldEntity = dupcDefectRepository.findByTaskIdAndRelPath(uploadDefectVO.getTaskId(), dupcDefectEntity.getRelPath());
        // 已经存在的风险文件
        if (oldEntity != null)
        {
            Integer oldStatus = oldEntity.getStatus();

            // 如果风险文件状态是new且风险系数低于基线，将文件置为已修复
            if (oldStatus == DefectConstants.DefectStatus.NEW.value() && dupRate < m)
            {
                dupcDefectEntity.setStatus(DefectConstants.DefectStatus.FIXED.value());
                dupcDefectEntity.setFixedTime(curTime);
            }
            // 如果风险文件状态是closed且风险系数高于基线，将文件置为new
            else if ((oldStatus & DefectConstants.DefectStatus.FIXED.value()) > 0 && dupRate >= m)
            {
                dupcDefectEntity.setStatus(oldStatus - DefectConstants.DefectStatus.FIXED.value());
            }
            else
            {
                dupcDefectEntity.setStatus(oldStatus);
            }
        }
        // 新增的风险文件
        else
        {
            dupcDefectEntity.setCreateTime(curTime);
            if (dupRate < m)
            {
                dupcDefectEntity.setStatus(DefectConstants.DefectStatus.FIXED.value());
                dupcDefectEntity.setFixedTime(curTime);
            }
            else
            {
                dupcDefectEntity.setStatus(DefectConstants.DefectStatus.NEW.value());
            }
        }

        dupcDefectRepository.save(dupcDefectEntity);
    }


    /**
     * 处理重复代码块信息
     *
     * @param dupcDefectEntity
     */
    private void handleDupBlock(DUPCDefectEntity dupcDefectEntity)
    {
        List<CodeBlockEntity> blockList = dupcDefectEntity.getBlockList();
        if (CollectionUtils.isNotEmpty(blockList))
        {
            for (CodeBlockEntity block : blockList)
            {
                block.setSourceFile(dupcDefectEntity.getFilePath());
            }
        }
    }
}

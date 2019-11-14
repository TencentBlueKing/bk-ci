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

package com.tencent.bk.codecc.job.service.impl;

import com.tencent.bk.codecc.defect.model.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.LintDefectEntity;
import com.tencent.bk.codecc.defect.model.LintFileEntity;
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.bk.codecc.defect.vo.common.CommonAuthorTransVO;
import com.tencent.bk.codecc.job.dao.defect.query.CCNDefectRepository;
import com.tencent.bk.codecc.job.dao.defect.query.LintFileQueryRepository;
import com.tencent.bk.codecc.job.dao.defect.query.TransferAuthorRepository;
import com.tencent.bk.codecc.job.service.MultiToolService;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 多工具服务实现
 *
 * @version V1.0
 * @date 2019/5/29
 */
@Service
public class MultiToolServiceImpl implements MultiToolService
{
    private static Logger logger = LoggerFactory.getLogger(MultiToolServiceImpl.class);

    @Autowired
    private LintFileQueryRepository lintFileQueryRepository;

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Autowired
    private TransferAuthorRepository transferAuthorRepository;

    @Override
    public void transferAuthor(CommonAuthorTransVO commonAuthorTransVO)
    {
        TransferAuthorEntity transferAuthorEntity = transferAuthorRepository.findFirstByTaskIdAndToolName(commonAuthorTransVO.getTaskId(),
                commonAuthorTransVO.getToolName());
        if (null == transferAuthorEntity)
        {
            transferAuthorEntity = new TransferAuthorEntity();
            transferAuthorEntity.setTaskId(commonAuthorTransVO.getTaskId());
            transferAuthorEntity.setToolName(commonAuthorTransVO.getToolName());
        }
        List<TransferAuthorEntity.TransferAuthorPair> transferAuthorPairs = transferAuthorEntity.getTransferAuthorList();
        if (CollectionUtils.isEmpty(transferAuthorPairs))
        {
            transferAuthorPairs = new ArrayList<>();
            for (int i = 0; i < commonAuthorTransVO.getSourceAuthor().size(); i++)
            {
                transferAuthorPairs.add(new TransferAuthorEntity.TransferAuthorPair(commonAuthorTransVO.getSourceAuthor().get(i),
                        commonAuthorTransVO.getTargetAuthor().get(i)));
            }
        }
        else
        {
            transferAuthorEntity.setTransferAuthorList(transferAuthorPairs.stream()
                    .map(transferAuthorPair ->
                    {
                        String sourceAuthor = transferAuthorPair.getSourceAuthor();
                        if (commonAuthorTransVO.getSourceAuthor().contains(sourceAuthor))
                        {
                            int index = commonAuthorTransVO.getSourceAuthor().indexOf(sourceAuthor);
                            return new TransferAuthorEntity.TransferAuthorPair(commonAuthorTransVO.getSourceAuthor().get(index),
                                    commonAuthorTransVO.getTargetAuthor().get(index));
                        }
                        else
                        {
                            return transferAuthorPair;
                        }
                    })
                    .collect(Collectors.toList()));
        }
        transferAuthorRepository.save(transferAuthorEntity);

        if (ComConstants.Tool.CCN.name().equalsIgnoreCase(commonAuthorTransVO.getToolName()))
        {
            logger.info("ccn tool batch transfer author, task id: {}", commonAuthorTransVO.getTaskId());
            List<CCNDefectEntity> ccnDefectEntityList = ccnDefectRepository.findNotRepairedDefect(commonAuthorTransVO.getTaskId(),
                    ComConstants.DEFECT_STATUS_CLOSED);
            refreshDefectAuthor4CCN(commonAuthorTransVO.getSourceAuthor(), commonAuthorTransVO.getTargetAuthor(), ccnDefectEntityList);
        }
        else
        {
            logger.info("lint tool batch transfer author, task id: {}, tool name: {}", commonAuthorTransVO.getTaskId(),
                    commonAuthorTransVO.getToolName());
            List<LintFileEntity> lintFileEntityList = lintFileQueryRepository.findByTaskIdAndToolNameAndStatus(commonAuthorTransVO.getTaskId(),
                    commonAuthorTransVO.getToolName(), ComConstants.TaskFileStatus.NEW.value());

            if (CollectionUtils.isNotEmpty(lintFileEntityList))
            {
                lintFileEntityList.forEach(
                        lintFileEntity ->
                        {
                            refreshDefectAuthor(commonAuthorTransVO.getSourceAuthor(),
                                    commonAuthorTransVO.getTargetAuthor(),
                                    lintFileEntity);
                            lintFileQueryRepository.save(lintFileEntity);
                        }
                );
            }
        }


    }


    private void refreshDefectAuthor(List<String> sourceAuthor, List<String> targetAuthor, LintFileEntity lintFileEntity)
    {
        if (CollectionUtils.isEmpty(sourceAuthor))
        {
            logger.info("previous author is empty, no operation conducted");
            return;
        }
        //1.设置lint文件中的作者清单
        Set<String> authorList = lintFileEntity.getAuthorList();
        lintFileEntity.setAuthorList(authorList.stream()
                .map(author ->
                {
                    if (sourceAuthor.contains(author))
                    {
                        return targetAuthor.get(sourceAuthor.indexOf(author));
                    }
                    else
                    {
                        return author;
                    }
                })
                .collect(Collectors.toSet()));
        //2.设置告警清单中的作者
        List<LintDefectEntity> lintDefectEntityList = lintFileEntity.getDefectList();
        lintDefectEntityList.forEach(lintDefectEntity ->
        {
            String author = lintDefectEntity.getAuthor();
            if (StringUtils.isNotEmpty(author))
            {
                if (sourceAuthor.contains(author))
                {
                    lintDefectEntity.setAuthor(targetAuthor.get(sourceAuthor.indexOf(author)));
                }
            }
        });
    }


    private void refreshDefectAuthor4CCN(List<String> sourceAuthor, List<String> targetAuthor, List<CCNDefectEntity> ccnDefectEntityList)
    {
        if (CollectionUtils.isEmpty(sourceAuthor))
        {
            logger.info("previous author is empty, no operation conducted");
            return;
        }
        ccnDefectEntityList.forEach(ccnDefectEntity ->
                {
                    String author = ccnDefectEntity.getAuthor();
                    if (StringUtils.isNotEmpty(author))
                    {
                        logger.info("author is {}", author);
                        if (sourceAuthor.contains(author))
                        {
                            logger.info("target author is {}", targetAuthor.get(sourceAuthor.indexOf(author)));
                            ccnDefectEntity.setAuthor(targetAuthor.get(sourceAuthor.indexOf(author)));
                            ccnDefectRepository.save(ccnDefectEntity);
                        }
                    }
                }
        );
    }
}

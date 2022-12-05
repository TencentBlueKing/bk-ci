/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.codeccjob.consumer;

import com.tencent.bk.codecc.defect.model.TransferAuthorEntity;
import com.tencent.bk.codecc.defect.vo.common.AuthorTransferVO;
import com.tencent.bk.codecc.codeccjob.dao.mongorepository.TransferAuthorRepository;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 多工具处理消息消费类
 *
 * @version V1.0
 * @date 2019/5/29
 */
@Component
@Slf4j
public class AuthorTransferConsumer
{
    @Autowired
    private TransferAuthorRepository transferAuthorRepository;

    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;

    /**
     * 批量作者转换
     *
     * @param authorTransferVO
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_AUTHOR_TRANS,
            value = @Queue(value = QUEUE_AUTHOR_TRANS, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_AUTHOR_TRANS, durable = "true", delayed = "true", type = "topic")))
    public void batchAuthorTrans(AuthorTransferVO authorTransferVO)
    {
        log.info("begin to batch transfer author：{}", authorTransferVO);
        try
        {
            transferAuthor(authorTransferVO);
        }
        catch (Exception e)
        {
            log.error("execute author transfer fail!\n{}", authorTransferVO);
        }
    }

    public void transferAuthor(AuthorTransferVO authorTransferVO)
    {
        // 保存作者转换关系
        List<AuthorTransferVO.TransferAuthorPair> needRefreshAuthorTransferPairs = saveTransferAuthor(authorTransferVO);
        log.info("needRefreshAuthorTransferPairs:{}", needRefreshAuthorTransferPairs);
        if (CollectionUtils.isEmpty(needRefreshAuthorTransferPairs))
        {
            return;
        }

        List<String> tools = authorTransferVO.getEffectiveTools();
        authorTransferVO.setTransferAuthorList(needRefreshAuthorTransferPairs);
        // 工具维度的所有类型告警数据都需要更新
        tools.forEach(toolName ->
                {
                    if (!ComConstants.Tool.DUPC.name().equals(toolName) && !ComConstants.Tool.CLOC.name().equals(toolName))
                    {
                        log.info("toolName:{}", toolName);
                        authorTransferVO.setToolName(toolName);
                        IBizService bizService = bizServiceFactory
                                .createBizService(toolName, ComConstants.BusinessType.AUTHOR_TRANS.value(), IBizService.class);
                        bizService.processBiz(authorTransferVO);
                    }
                }
        );
        log.info("success batch transfer author");
    }

    /**
     * 保存转换关系
     *
     * @param authorTransferVO
     * @return
     */
    private List<AuthorTransferVO.TransferAuthorPair> saveTransferAuthor(AuthorTransferVO authorTransferVO)
    {
        TransferAuthorEntity transferAuthorEntity = transferAuthorRepository.findFirstByTaskId(authorTransferVO.getTaskId());
        Map<List<String>, List<String>> oldTransferAuthorPairMap = new HashMap<>();
        if (transferAuthorEntity != null && CollectionUtils.isNotEmpty(transferAuthorEntity.getTransferAuthorList()))
        {
            List<TransferAuthorEntity.TransferAuthorPair> oldTransferAuthorList = transferAuthorEntity.getTransferAuthorList();
            oldTransferAuthorList.forEach(transferAuthorPair ->
            {
                List<String> sourceAuthorList = Arrays.asList(transferAuthorPair.getSourceAuthor().split(ComConstants.SEMICOLON));
                List<String> targetAuthorList = Arrays.asList(transferAuthorPair.getTargetAuthor().split(ComConstants.SEMICOLON));
                oldTransferAuthorPairMap.put(sourceAuthorList, targetAuthorList);
            });
        }

        List<AuthorTransferVO.TransferAuthorPair> needRefreshAuthorTransferPairs = new ArrayList<>();
        List<AuthorTransferVO.TransferAuthorPair> transferAuthorList = authorTransferVO.getTransferAuthorList();
        if (CollectionUtils.isNotEmpty(transferAuthorList))
        {
            transferAuthorList.forEach(transferAuthorPair ->
            {
                List<String> newSourceAuthorList = Arrays.asList(transferAuthorPair.getSourceAuthor().split(ComConstants.SEMICOLON));
                List<String> newTargetAuthorList = Arrays.asList(transferAuthorPair.getTargetAuthor().split(ComConstants.SEMICOLON));

                boolean needRefresh = true;
                Set<Map.Entry<List<String>, List<String>>> entrySet = oldTransferAuthorPairMap.entrySet();
                for (Map.Entry<List<String>, List<String>> entry : entrySet)
                {
                    List<String> oldSourceAuthorList = entry.getKey();
                    List<String> oldTargetAuthorList = entry.getValue();

                    // 如果转换关系是之前已经存在的，则不需要重新刷新数据
                    if (CollectionUtils.isEqualCollection(newSourceAuthorList, oldSourceAuthorList)
                            && CollectionUtils.isEqualCollection(newTargetAuthorList, oldTargetAuthorList))
                    {
                        needRefresh = false;
                        break;
                    }
                }
                if (needRefresh)
                {
                    needRefreshAuthorTransferPairs.add(transferAuthorPair);
                }
            });
        }

        if (null == transferAuthorEntity)
        {
            transferAuthorEntity = new TransferAuthorEntity();
            transferAuthorEntity.setTaskId(authorTransferVO.getTaskId());
        }

        List<TransferAuthorEntity.TransferAuthorPair> newTransferAuthorList = null;
        if (CollectionUtils.isNotEmpty(transferAuthorList))
        {
            newTransferAuthorList = transferAuthorList.stream()
                    .map(authorPair ->
                    {
                        TransferAuthorEntity.TransferAuthorPair transferAuthorPair = new TransferAuthorEntity.TransferAuthorPair();
                        transferAuthorPair.setSourceAuthor(authorPair.getSourceAuthor());
                        transferAuthorPair.setTargetAuthor(authorPair.getTargetAuthor());
                        return transferAuthorPair;
                    })
                    .collect(Collectors.toList());
        }
        transferAuthorEntity.setTransferAuthorList(newTransferAuthorList);

        transferAuthorRepository.save(transferAuthorEntity);
        return needRefreshAuthorTransferPairs;
    }
}

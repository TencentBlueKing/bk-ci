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

package com.tencent.bk.codecc.defect.dao.redis;

import com.tencent.bk.codecc.defect.dto.AnalysisVersionDTO;
import com.tencent.devops.common.constant.RedisKeyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_ANALYSIS_VERSION;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_ANALYSIS_VERSION;

/**
 * 任务分析的redis持久化
 *
 * @version V1.0
 * @date 2019/5/7
 */
@Repository
public class TaskAnalysisDao
{
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static Logger logger = LoggerFactory.getLogger(TaskAnalysisDao.class);

    /**
     * 生成分析版本号
     * 生成规则：serialNum:yyyyMMddHHmmss
     *
     * @param taskId
     * @param toolName
     */
    public String generateAnalysisVersion(long taskId, String toolName)
    {
        //利用UUID生成版本号
        String version = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(String.format("%s%d%s%s", RedisKeyConstants.PREFIX_ANALYSIS_VERSION, taskId, ":", toolName), version);
        AnalysisVersionDTO analysisVersionDTO = new AnalysisVersionDTO();
        analysisVersionDTO.setTaskId(taskId);
        analysisVersionDTO.setToolName(toolName);
        analysisVersionDTO.setAnalysisVersion(version);
        rabbitTemplate.convertAndSend(EXCHANGE_ANALYSIS_VERSION, ROUTE_ANALYSIS_VERSION, analysisVersionDTO);

        return version;
    }

    /**
     * 获取当前分析版本号
     *
     * @param taskId
     * @param toolName
     * @return
     */
    public String getCurrentAnalysisVersion(long taskId, String toolName)
    {
        return redisTemplate.opsForValue().get(String.format("%s%d%s%s", RedisKeyConstants.PREFIX_ANALYSIS_VERSION, taskId, ":", toolName));
    }
}

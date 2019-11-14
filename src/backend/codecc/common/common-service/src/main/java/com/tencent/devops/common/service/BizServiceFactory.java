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

package com.tencent.devops.common.service;

import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 多工具业务处理器的工厂类
 *
 * @version V2.6
 * @date 2018/1/18
 */
public class BizServiceFactory<T>
{

    private static Logger logger = LoggerFactory.getLogger(BizServiceFactory.class);

    /**
     * 为不同类型的工具创建相应的数据报表处理器
     *
     * @param toolName
     * @return
     */
    public T createBizService(String toolName, String businessType, Class<T> clz)
    {
        StringRedisTemplate stringRredisTemplate = SpringContextUtil.Companion.getBean(StringRedisTemplate.class);
        Object pattern = stringRredisTemplate.opsForHash().get(RedisKeyConstants.PREFIX_TOOL + toolName.toUpperCase(), RedisKeyConstants.FILED_PATTERN);
        if (pattern == null)
        {
            logger.error("The system temporarily does not support tool [{}] and businessType [{}]", toolName, businessType);
            throw new CodeCCException(CommonMessageCode.NOT_FOUND_PROCESSOR);
        }

        // BizService的bean名（PatternBizTypeBizService）的后缀名,比如：COVERITYBatchMarkDefectBizService
        String beanName = String.format("%s%s%s", pattern, businessType, ComConstants.BIZ_SERVICE_POSTFIX);

        T processor = null;
        try
        {
            processor = SpringContextUtil.Companion.getBean(clz, beanName);
        }
        catch (BeansException e)
        {
            logger.error("Bean Name [{}] Not Found:", beanName);
        }

        // 如果没找到工具的具体处理类，则采用通用的处理器
        if (processor == null)
        {
            beanName = String.format("%s%s%s", ComConstants.COMMON_BIZ_SERVICE_PREFIX, businessType, ComConstants.BIZ_SERVICE_POSTFIX);
            try
            {
                processor = SpringContextUtil.Companion.getBean(clz, beanName);
            }
            catch (BeansException e)
            {
                logger.error("Bean Name [{}] Not Found:", beanName);
            }
        }

        if (processor == null)
        {
            logger.error("get bean name [{}] fail!", beanName);
            throw new CodeCCException(CommonMessageCode.NOT_FOUND_PROCESSOR);
        }

        return processor;
    }

}

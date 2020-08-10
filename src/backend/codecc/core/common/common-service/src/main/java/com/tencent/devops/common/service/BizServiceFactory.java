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

package com.tencent.devops.common.service;

import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 多工具业务处理器的工厂类
 *
 * @version V2.6
 * @date 2018/1/18
 */
@Slf4j
public class BizServiceFactory<T> {

    private static final String PREFIX_TASK_INFO = "TASK_INFO:";
    private static final String KEY_CREATE_FROM = "createFrom";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 为不同类型的工具创建相应的数据报表处理器
     *
     * @param toolName
     * @return
     */
    public T createBizService(String toolName, String businessType, Class<T> clz) {
        String toolProcessorBeanName = String.format("%s%s%s", toolName, businessType, ComConstants.BIZ_SERVICE_POSTFIX);
        String parrernProcessorBeanName = null;
        String commonProcessorBeanName = null;

        // 获取工具名称开头的处理类
        T processor = getProcessor(clz, toolProcessorBeanName);

        // 获取工具类型开头的处理类
        if (processor == null) {
            ToolMetaCacheService toolMetaCacheService = SpringContextUtil.Companion.getBean(ToolMetaCacheService.class);
            // BizService的bean名（PatternBizTypeBizService）的后缀名,比如：COVERITYBatchMarkDefectBizService
            parrernProcessorBeanName = String.format("%s%s%s", toolMetaCacheService.getToolPattern(toolName), businessType, ComConstants.BIZ_SERVICE_POSTFIX);
            processor = getProcessor(clz, parrernProcessorBeanName);
        }

        // 如果没找到工具的具体处理类，则采用通用的处理器
        if (processor == null) {
            commonProcessorBeanName = String.format("%s%s%s", ComConstants.COMMON_BIZ_SERVICE_PREFIX, businessType, ComConstants.BIZ_SERVICE_POSTFIX);
            processor = getProcessor(clz, commonProcessorBeanName);
        }

        if (processor == null) {
            log.error("get bean name [{}, {}, {}] fail!", toolProcessorBeanName, parrernProcessorBeanName, commonProcessorBeanName);
            throw new CodeCCException(CommonMessageCode.NOT_FOUND_PROCESSOR);
        }

        return processor;
    }


    /**
     * 为不同类型的业务创建相应的处理器
     *
     * @param businessType
     * @param clz
     * @return
     */
    public T createBizService(String businessType, Class<T> clz) {
        String beanName = String.format("%s%s", businessType, ComConstants.BIZ_SERVICE_POSTFIX);
        T processor = null;
        try {
            processor = SpringContextUtil.Companion.getBean(clz, beanName);
        } catch (BeansException e) {
            log.error("Bean Name [{}] Not Found:", beanName);
        }

        if (processor == null) {
            log.error("get bean name [{}] fail!", beanName);
            throw new CodeCCException(CommonMessageCode.NOT_FOUND_PROCESSOR);
        }

        return processor;
    }

    /**
     * 为不同创建来源创建相应的处理器
     *
     * @param taskId
     * @param businessType
     * @param clz
     * @return
     */
    public T createBizService(Long taskId, String businessType, Class<T> clz) {
        String createFrom = (String) redisTemplate.opsForHash().get(PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM);

        T processor = null;

        String createFromCommonBeanName = String.format("%s%s%s", ComConstants.COMMON_CREATE_FROM_BIZ_INFIX, businessType, ComConstants.BIZ_SERVICE_POSTFIX);
        String createFromGongFengBeanName = String.format("%s%s%s", ComConstants.GONGFENG_CREATE_FROM_BIZ_INFIX, businessType, ComConstants.BIZ_SERVICE_POSTFIX);

        // 获取创建来源是GongFeng名称开头的处理类
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(createFrom)) {
            processor = getProcessor(clz, createFromGongFengBeanName);
        }

        //如果不是来源工蜂，或者没有成功获取到GongFeng名称开头的，则都采用共同的处理器
        if (processor == null){
            processor = getProcessor(clz, createFromCommonBeanName);
        }

        return processor;
    }

    private <T> T getProcessor(Class<T> clz, String beanName) {
        T processor = null;
        try {
            processor = SpringContextUtil.Companion.getBean(clz, beanName);
        } catch (BeansException e) {
            //log.error("Bean Name [{}] Not Found:", beanName);
        }
        return processor;
    }
}

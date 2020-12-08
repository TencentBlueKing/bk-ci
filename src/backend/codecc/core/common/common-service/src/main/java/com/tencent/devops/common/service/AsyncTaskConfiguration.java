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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步执行配置类
 *
 * @version V1.0
 * @date 2019/5/6
 */
@Configuration
@EnableAsync
public class AsyncTaskConfiguration
{

    private static Logger logger = LoggerFactory.getLogger(AsyncTaskConfiguration.class);

    /**
     * 定义线程执行器
     *
     * @return
     */
    @Bean
    public Executor asyncTaskExecutor()
    {
        logger.info("initialize async task thread pool");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //配置核心线程数
        executor.setCorePoolSize(20);
        //配置最大线程数
        executor.setMaxPoolSize(20);
        //配置队列长度
        executor.setQueueCapacity(200);
        //设置线程空闲时间
        executor.setKeepAliveSeconds(30);
        //线程名前缀
        executor.setThreadNamePrefix("codecc-");
        //线程池拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //执行初始化
        executor.initialize();
        return executor;
    }



    @Bean
    public Executor asyncReportDefectExecutor()
    {
        logger.info("initialize async task thread pool");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //配置核心线程数(defect服务不需要)
        executor.setCorePoolSize(100);
        //设置队列大小
        executor.setQueueCapacity(100);
        //配置最大线程数
        executor.setMaxPoolSize(300);
        //设置线程空闲时间
        executor.setKeepAliveSeconds(30);
        //对core线程也有用
//        executor.setAllowCoreThreadTimeOut(true);
        //线程名前缀
        executor.setThreadNamePrefix("reportDefect-");
        //线程池拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //执行初始化
        executor.initialize();
        return executor;
    }

    @Bean
    public Executor asyncLintDefectTracingExecutor()
    {
        logger.info("initialize async lint defect tracing pool");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //配置核心线程数(defect服务不需要)
        executor.setCorePoolSize(100);
        //设置队列大小
        executor.setQueueCapacity(100);
        //配置最大线程数
        executor.setMaxPoolSize(300);
        //设置线程空闲时间
        executor.setKeepAliveSeconds(30);
        //线程名前缀
        executor.setThreadNamePrefix("lint-defect-tracing-");
        //线程池拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //执行初始化
        executor.initialize();
        return executor;
    }


    @Bean
    public Executor asyncCcnDefectTracingExecutor()
    {
        logger.info("initialize async ccn defect tracing pool");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //配置核心线程数(defect服务不需要)
        executor.setCorePoolSize(100);
        //设置队列大小
        executor.setQueueCapacity(100);
        //配置最大线程数
        executor.setMaxPoolSize(300);
        //设置线程空闲时间
        executor.setKeepAliveSeconds(30);
        //线程名前缀
        executor.setThreadNamePrefix("ccn-defect-tracing-");
        //线程池拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //执行初始化
        executor.initialize();
        return executor;
    }
}

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

import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
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

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 路径屏蔽消费者
 *
 * @version V1.0
 * @date 2019/5/21
 */
@Component
@Slf4j
public class FilterPathConsumer
{
    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;

    /**
     * 刪除屏蔽路徑
     *
     * @param filterPathInput
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_DEL_TASK_FILTER_PATH,
            value = @Queue(value = QUEUE_DEL_TASK_FILTER_PATH, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_TASK_FILTER_PATH, durable = "true", delayed = "true", type = "topic")))
    public void delFilterPathMessage(FilterPathInputVO filterPathInput)
    {
        log.info("delete path ignore: {}", filterPathInput);
        // status設置成NEW
        filterPathInput.setAddFile(Boolean.FALSE);
        try
        {
            doFilterPathMessage(filterPathInput);
        }
        catch (Exception e)
        {
            log.error("delFilterPath exception. \n{}", filterPathInput, e);
        }
    }

    /**
     * 添加屏蔽路徑
     *
     * @param filterPathInput
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_ADD_TASK_FILTER_PATH,
            value = @Queue(value = QUEUE_ADD_TASK_FILTER_PATH, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_TASK_FILTER_PATH, durable = "true", delayed = "true", type = "topic")))
    public void addFilterPathMessage(FilterPathInputVO filterPathInput)
    {
        log.info("add path ignore: {}", filterPathInput);
        // status設置成EXCLUDED
        filterPathInput.setAddFile(Boolean.TRUE);
        try
        {
            doFilterPathMessage(filterPathInput);
        }
        catch (Exception e)
        {
            log.error("addFilterPath exception. \n{}", filterPathInput, e);
        }
    }


    private void doFilterPathMessage(FilterPathInputVO filterPathInputVO)
    {
        if (Objects.nonNull(filterPathInputVO) && Objects.nonNull(filterPathInputVO.getAddFile()))
        {
            Set<String> ignoredPaths = new HashSet<>();
            // 屏蔽默认路径
            if (ComConstants.PATH_TYPE_DEFAULT.equalsIgnoreCase(filterPathInputVO.getPathType()))
            {
                ignoredPaths.addAll(filterPathInputVO.getDefaultFilterPath());
            }
            else
            {
                List<String> fileDir = filterPathInputVO.getFilterDir();
                List<String> filterFile = filterPathInputVO.getFilterFile();
                List<String> customPath = filterPathInputVO.getCustomPath();

                // 手工输入路径
                if (!CollectionUtils.isEmpty(customPath))
                {
                    ignoredPaths.addAll(customPath);
                }
                // 选择屏蔽文件
                if (!CollectionUtils.isEmpty(filterFile))
                {
                    ignoredPaths.addAll(filterFile);
                }
                // 选择屏蔽文件夹
                if (!CollectionUtils.isEmpty(fileDir))
                {
                    ignoredPaths.addAll(fileDir);
                }
                // code.yml屏蔽路径
                if (!CollectionUtils.isEmpty(filterPathInputVO.getTestSourceFilterPath()))
                {
                    ignoredPaths.addAll(filterPathInputVO.getTestSourceFilterPath());
                }
                if (!CollectionUtils.isEmpty(filterPathInputVO.getAutoGenFilterPath()))
                {
                    ignoredPaths.addAll(filterPathInputVO.getAutoGenFilterPath());
                }
                if (!CollectionUtils.isEmpty(filterPathInputVO.getThirdPartyFilterPath()))
                {
                    ignoredPaths.addAll(filterPathInputVO.getThirdPartyFilterPath());
                }
            }

            if (CollectionUtils.isEmpty(ignoredPaths))
            {
                log.debug("IgnoredPaths list is empty!");
                return;
            }

            filterPathInputVO.setFilterPaths(ignoredPaths);

            List<String> tools = filterPathInputVO.getEffectiveTools();
            // 工具维度的所有类型告警数据都需要更新
            tools.forEach(toolName ->
                    {
                        log.info("begin filter path for: {}", toolName);
                        filterPathInputVO.setToolName(toolName);
                        IBizService bizService = bizServiceFactory
                                .createBizService(toolName, ComConstants.BusinessType.FILTER_PATH.value(), IBizService.class);
                        bizService.processBiz(filterPathInputVO);
                        log.info("end filter path for: {}", toolName);
                    }
            );
        }
    }

    private void doDefectPath(FilterPathInputVO filterPathInput, Set<String> ignoredPaths)
    {

    }

}

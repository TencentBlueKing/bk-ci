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

import com.tencent.bk.codecc.codeccjob.service.TaskPersonalStatisticService;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_TASK_FILTER_PATH;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_ADD_TASK_FILTER_PATH;
import static com.tencent.devops.common.web.mq.ConstantsKt.QUEUE_DEL_TASK_FILTER_PATH;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_ADD_TASK_FILTER_PATH;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_DEL_TASK_FILTER_PATH;

/**
 * 路径屏蔽消费者
 *
 * @version V1.0
 * @date 2019/5/21
 */
@Component
@Slf4j
public class FilterPathConsumer {
    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;

    @Autowired
    private TaskPersonalStatisticService taskPersonalStatisticService;

    /**
     * 刪除屏蔽路徑
     *
     * @param filterPathInput
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_DEL_TASK_FILTER_PATH,
            value = @Queue(value = QUEUE_DEL_TASK_FILTER_PATH, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_TASK_FILTER_PATH, durable = "true", delayed = "true", type = "topic")))
    public void delFilterPathMessage(FilterPathInputVO filterPathInput) {
        log.info("delete path ignore: {}", filterPathInput);
        // status設置成NEW
        filterPathInput.setAddFile(Boolean.FALSE);
        try {
            doFilterPathMessage(filterPathInput);
        } catch (Exception e) {
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
    public void addFilterPathMessage(FilterPathInputVO filterPathInput) {
        log.info("add path ignore: {}", filterPathInput);
        // status設置成EXCLUDED
        filterPathInput.setAddFile(Boolean.TRUE);
        try {
            doFilterPathMessage(filterPathInput);
        } catch (Exception e) {
            log.error("addFilterPath exception. \n{}", filterPathInput, e);
        }
    }

    private void doFilterPathMessage(FilterPathInputVO filterPathInputVO) {
        if (Objects.nonNull(filterPathInputVO) && Objects.nonNull(filterPathInputVO.getAddFile())) {
            List<String> tools = filterPathInputVO.getEffectiveTools();

            Set<String> ignoredPaths = getFilterPaths(filterPathInputVO);
            if (CollectionUtils.isEmpty(ignoredPaths)) {
                log.debug("IgnoredPaths list is empty!");
                return;
            }

            filterPathInputVO.setFilterPaths(ignoredPaths);

            // 工具维度的所有类型告警数据都需要更新
            tools.forEach(toolName -> {
                log.info("begin filter path for: {}", toolName);
                filterPathInputVO.setToolName(toolName);
                IBizService bizService = bizServiceFactory
                        .createBizService(toolName, ComConstants.BusinessType.FILTER_PATH.value(), IBizService.class);
                bizService.processBiz(filterPathInputVO);
                log.info("end filter path for: {}", toolName);
            });

            // update overview data
            taskPersonalStatisticService.refresh(filterPathInputVO.getTaskId(), "from FilterPathConsumer doFilterPathMessage");
        }
    }

    @NotNull
    private Set<String> getFilterPaths(FilterPathInputVO filterPathInputVO) {
        Set<String> ignoredPaths = new HashSet<>();
        // 屏蔽默认路径
        if (ComConstants.PATH_TYPE_DEFAULT.equalsIgnoreCase(filterPathInputVO.getPathType())) {
            ignoredPaths.addAll(filterPathInputVO.getDefaultFilterPath());
        } else if (ComConstants.PATH_TYPE_CODE_YML.equalsIgnoreCase(filterPathInputVO.getPathType())) {
            // code.yml屏蔽路径
            if (CollectionUtils.isNotEmpty(filterPathInputVO.getAutoGenFilterPath())) {
                ignoredPaths.addAll(filterPathInputVO.getAutoGenFilterPath());
            }
            if (CollectionUtils.isNotEmpty(filterPathInputVO.getThirdPartyFilterPath())) {
                ignoredPaths.addAll(filterPathInputVO.getThirdPartyFilterPath());
            }
            if (CollectionUtils.isNotEmpty(filterPathInputVO.getTestSourceFilterPath())) {
                ignoredPaths.addAll(filterPathInputVO.getTestSourceFilterPath());
            }
        } else {
            // 手工输入路径
            if (CollectionUtils.isNotEmpty(filterPathInputVO.getCustomPath())) {
                ignoredPaths.addAll(filterPathInputVO.getCustomPath());
            }
            // 选择屏蔽文件
            if (CollectionUtils.isNotEmpty(filterPathInputVO.getFilterFile())) {
                ignoredPaths.addAll(filterPathInputVO.getFilterFile());
            }
            // 选择屏蔽文件夹
            if (CollectionUtils.isNotEmpty(filterPathInputVO.getFilterDir())) {
                ignoredPaths.addAll(filterPathInputVO.getFilterDir());
            }
        }

        return ignoredPaths;
    }
}

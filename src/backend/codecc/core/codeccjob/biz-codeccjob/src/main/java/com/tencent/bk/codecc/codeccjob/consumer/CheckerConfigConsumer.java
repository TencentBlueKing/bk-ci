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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.codeccjob.service.TaskPersonalStatisticService;
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
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

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

/**
 * 规则配置消费者 [ 当前仅仅操作lint类的规则配置 ]
 *
 * @version V1.0
 * @date 2019/6/17
 */
@Component
@Slf4j
public class CheckerConfigConsumer
{
    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;

    @Autowired
    private TaskPersonalStatisticService taskPersonalStatisticService;

    /**
     * 添加屏蔽规则 [ 加入屏蔽规则列表t_ignore_checker之后, 调用Job模块操作]
     * <p>
     * 1.  根据相同的屏蔽规则更新t_lint_defect
     * 1.1 从规则列表中移除屏蔽规则
     * 1.2 从告警列表中移除屏蔽规则相同的告警
     * 1.3 重新从告警列表中统计作者列表
     * <p>
     * 2. 统计移除之后的告警
     * 2.1 告警总数
     * 2.2 新的告警总数
     * 2.3 历史告警总数
     * <p>
     * 3. 更新数据报表的数据：LintStatisticEntity
     *
     * @param model
     */
    @RabbitListener(bindings = @QueueBinding(key = ROUTE_IGNORE_CHECKER,
            value = @Queue(value = QUEUE_IGNORE_CHECKER, durable = "true"),
            exchange = @Exchange(value = EXCHANGE_TASK_CHECKER_CONFIG, durable = "true", delayed = "true", type = "topic")))
    public void checkerConfig(ConfigCheckersPkgReqVO model)
    {
        log.info("checker config job start ... data: \n{}", model);
        if (CollectionUtils.isEmpty(model.getClosedCheckers()) && CollectionUtils.isEmpty(model.getOpenedCheckers()))
        {
            return;
        }

        try
        {
            Set<String> closeSet = Sets.newHashSet();
            Set<String> opendSet = Sets.newHashSet();
            Optional.ofNullable(model.getClosedCheckers()).orElseGet(ArrayList::new).forEach(checker -> addChecker(closeSet, checker));
            Optional.ofNullable(model.getOpenedCheckers()).orElseGet(ArrayList::new).forEach(checker -> addChecker(opendSet, checker));
            model.setClosedCheckers(Lists.newArrayList(closeSet));
            model.setOpenedCheckers(Lists.newArrayList(opendSet));

            IBizService bizService = bizServiceFactory.createBizService(model.getToolName(), ComConstants.BusinessType.CONFIG_PKG.value(), IBizService.class);
            bizService.processBiz(model);

            taskPersonalStatisticService.refresh(model.getTaskId(), "from v3 checker set of set relationships");
        }
        catch (Exception e)
        {
            log.error("checkerConfig exception. \n{}", model, e);
        }
    }

    /**
     * 添加规则
     *
     * @param closeCheckers
     * @param checker
     */
    private void addChecker(Set<String> closeCheckers, String checker)
    {
        String checkerValue = checker.endsWith("-tosa") ?
                checker.substring(0, checker.indexOf("-tosa")) : checker;
        closeCheckers.add(checkerValue);
    }


}

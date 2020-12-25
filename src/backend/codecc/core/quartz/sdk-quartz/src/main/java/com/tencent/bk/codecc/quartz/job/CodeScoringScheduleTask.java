package com.tencent.bk.codecc.quartz.job;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_SCORING_OPENSOURCE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_SCORING_OPENSOURCE;

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

public class CodeScoringScheduleTask implements IScheduleTask {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void executeTask(@NotNull QuartzJobContext quartzJobContext) {
        rabbitTemplate.convertAndSend(EXCHANGE_SCORING_OPENSOURCE, ROUTE_SCORING_OPENSOURCE, "");
    }
}

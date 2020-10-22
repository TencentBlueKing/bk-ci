package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.pojo.AggregateDispatchFileName;
import com.tencent.bk.codecc.defect.service.IMessageQueueBizService;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.tencent.devops.common.web.mq.ConstantsKt.*;

@Slf4j
@Service("GongFengMessageQueueBizService")
public class GongFengMessageQueueBizServiceImpl implements IMessageQueueBizService {

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Qualifier("opensourceAsyncRabbitTemplate")
    @Autowired
    private AsyncRabbitTemplate asyncRabbitTemplate;

    @Override
    public Map<String, String> getExchangeAndRoutingKey(Long fileSize, String toolPattern) {

        log.warn("工蜂项目: {}", fileSize);
        String exchange = String.format("%s%s.opensource", PREFIX_EXCHANGE_DEFECT_COMMIT, toolPattern.toLowerCase());
        String routingKey = String.format("%s%s.opensource", PREFIX_ROUTE_DEFECT_COMMIT, toolPattern.toLowerCase());

        Map<String, String> messageQueueInfo = new HashMap<>();
        messageQueueInfo.put(EXCHANGE, exchange);
        messageQueueInfo.put(ROUTINGKEY, routingKey);

        return messageQueueInfo;
    }

    @Override
    public void messageQueueConvertAndSend(String toolName, CommitDefectVO commitDefectVO) {
        rabbitTemplate.convertAndSend(PREFIX_EXCHANGE_OPENSOURCE_DEFECT_COMMIT + toolName.toLowerCase(),
                PREFIX_ROUTE_OPENSOURCE_DEFECT_COMMIT + toolName.toLowerCase(), commitDefectVO);
    }

    @Override
    public AsyncRabbitTemplate.RabbitConverterFuture<Boolean> messageAsyncMsgFuture(AggregateDispatchFileName aggregateFileName) {

        AsyncRabbitTemplate.RabbitConverterFuture<Boolean> asyncMsgFuture =
                asyncRabbitTemplate.convertSendAndReceive(EXCHANGE_CLUSTER_ALLOCATION_OPENSOURCE, ROUTE_CLUSTER_ALLOCATION_OPENSOURCE, aggregateFileName);
        return asyncMsgFuture;
    }
}

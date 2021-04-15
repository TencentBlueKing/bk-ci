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
@Service("CommonMessageQueueBizService")
public class CommonMessageQueueBizServiceImpl implements IMessageQueueBizService {

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Qualifier("clusterAsyncRabbitTamplte")
    @Autowired
    private AsyncRabbitTemplate asyncRabbitTemplate;

    @Override
    public Map<String, String> getExchangAndEroutingKey(Long fileSize, String toolPattern) {

        String exchange;
        String routingKey;

        // 告警文件大于1G，告警文件不处理，在分析步骤中提示告警文件过大
        if (fileSize > 1024 * 1024 * 1024) {
            log.warn("告警文件大小超过1G: {}", fileSize);
            exchange = EXCHANGE_DEFECT_COMMIT_SUPER_LARGE;
            routingKey = ROUTE_DEFECT_COMMIT_SUPER_LARGE;
        }
        // 告警文件大于200M，小于1G，走大项目专用提单消息队列
        else if (fileSize > 1024 * 1024 * 200 && fileSize < 1024 * 1024 * 1024) {
            log.warn("告警文件大于200M小于1G: {}", fileSize);
            exchange = String.format("%s%s.large", PREFIX_EXCHANGE_DEFECT_COMMIT, toolPattern.toLowerCase());
            routingKey = String.format("%s%s.large", PREFIX_ROUTE_DEFECT_COMMIT, toolPattern.toLowerCase());
        }
        else {
            log.info("告警文件小于200M: {}", fileSize);
            exchange = String.format("%s%s.new", PREFIX_EXCHANGE_DEFECT_COMMIT, toolPattern.toLowerCase());
            routingKey = String.format("%s%s.new", PREFIX_ROUTE_DEFECT_COMMIT, toolPattern.toLowerCase());
        }

        Map<String, String> messageQueueInfo = new HashMap<>();
        messageQueueInfo.put(EXCHANGE, exchange);
        messageQueueInfo.put(ROUTINGKEY, routingKey);
        return messageQueueInfo;
    }

    @Override
    public void messageQueueConvertAndSend(String toolName, CommitDefectVO commitDefectVO) {
        rabbitTemplate.convertAndSend(PREFIX_EXCHANGE_DEFECT_COMMIT + toolName.toLowerCase(),
                PREFIX_ROUTE_DEFECT_COMMIT + toolName.toLowerCase(), commitDefectVO);
    }

    @Override
    public AsyncRabbitTemplate.RabbitConverterFuture<Boolean> messageAsyncMsgFuture(AggregateDispatchFileName aggregateFileName) {
        AsyncRabbitTemplate.RabbitConverterFuture<Boolean> asyncMsgFuture =
                asyncRabbitTemplate.convertSendAndReceive(EXCHANGE_CLUSTER_ALLOCATION, ROUTE_CLUSTER_ALLOCATION, aggregateFileName);

        return asyncMsgFuture;
    }
}

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.pojo.AggregateDispatchFileName;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;

import java.util.Map;

public interface IMessageQueueBizService {

    Map<String, String> getExchangAndEroutingKey(Long fileSize, String toolPattern);

    void messageQueueConvertAndSend(String toolName, CommitDefectVO commitDefectVO);

    AsyncRabbitTemplate.RabbitConverterFuture<Boolean> messageAsyncMsgFuture(AggregateDispatchFileName aggregateFileName);
}

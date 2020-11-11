package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.quartz.pojo.OperationType;
import com.tencent.bk.codecc.task.pojo.EmailMessageModel;
import com.tencent.bk.codecc.task.pojo.EmailNotifyModel;
import com.tencent.bk.codecc.task.pojo.RtxNotifyModel;
import com.tencent.bk.codecc.task.pojo.WeChatMessageModel;
import com.tencent.bk.codecc.task.service.EmailNotifyService;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class MockEmailNotifyServiceImpl implements EmailNotifyService {
    @Override
    public void sendReport(EmailNotifyModel emailNotifyModel) {

    }

    @Override
    public void sendRtx(RtxNotifyModel rtxNotifyModel) {

    }

    @Override
    public String addEmailScheduleTask(Long taskId, Set<Integer> week, Integer hour, OperationType operationType, String jobName) {
        return "";
    }

    @Override
    public void sendEmail(EmailMessageModel emailMessageModel) {

    }

    @Override
    public void sendWeChat(WeChatMessageModel weChatMessageModel) {

    }
}

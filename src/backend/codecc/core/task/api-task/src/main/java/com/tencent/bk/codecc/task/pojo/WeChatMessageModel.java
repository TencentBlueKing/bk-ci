package com.tencent.bk.codecc.task.pojo;

import com.tencent.devops.common.constant.ComConstants;
import lombok.Data;

import java.util.Map;

@Data
public class WeChatMessageModel {
    private ComConstants.WeChatNotifyTemplate template;
    private Map<String, String> contentParam;
}

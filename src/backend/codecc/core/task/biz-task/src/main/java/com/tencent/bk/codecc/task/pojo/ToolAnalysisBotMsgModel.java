package com.tencent.bk.codecc.task.pojo;

import lombok.Data;

import java.util.Set;

/**
 * 企业微信机器人消息
 */
@Data
public class ToolAnalysisBotMsgModel {
    /**
     * 消息体
     */
    private String bodyContent;

    /**
     * 被提醒人(at人)
     */
    private Set<String> authorSet;


    /**
     * 零告警匹配标识位，true则不发消息
     */
    private Boolean zeroDefectMatch;

}

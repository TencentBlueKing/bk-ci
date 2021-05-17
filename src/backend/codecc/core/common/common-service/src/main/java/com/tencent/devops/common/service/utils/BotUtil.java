package com.tencent.devops.common.service.utils;

import com.google.common.collect.Maps;
import com.tencent.devops.common.util.OkhttpUtils;
import org.apache.commons.collections.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

public class BotUtil {
    /**
     * 群机器人外网地址
     */
    private static final String BOT_OUTER_HOST = "https://qyapi.weixin.qq.com";

    /**
     * 群机器人内网地址
     */
    private static final String BOT_INNER_HOST = "http://in.qyapi.weixin.qq.com";

    /**
     * 发送消息到群机器人
     *
     * @param url
     * @param requestContent
     * @param authors
     */
    public static void sendMsgToRobot(String url, String requestContent, Set<String> authors) {
        // 机器人外网地址转换为内网地址
        if (url.contains(BOT_OUTER_HOST)) {
            url = url.replace(BOT_OUTER_HOST, BOT_INNER_HOST);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msgtype", "text");
        JSONObject textJson = new JSONObject();
        jsonObject.put("text", textJson);
        textJson.put("content", requestContent);
        JSONArray mentionedList = new JSONArray();
        textJson.put("mentioned_list", mentionedList);
        if (CollectionUtils.isNotEmpty(authors)) {
            for (String author : authors) {
                mentionedList.put(author);
            }
        }

        post(url, jsonObject.toString());
    }

    /**
     * POST Wrapper
     *
     * @param url
     * @param requestContent
     * @return
     */
    private static String post(String url, String requestContent) {
        Map<String, String> headerMap = Maps.newHashMap();
        headerMap.put("Content-Type", "application/json");
        return OkhttpUtils.INSTANCE.doHttpPost(url, requestContent, headerMap);
    }
}

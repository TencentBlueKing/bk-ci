package com.tencent.bk.codecc.defect.utils;

import com.google.common.collect.Maps;
import com.tencent.devops.common.util.OkhttpUtils;
import org.apache.commons.collections.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * 微信群机器人工具类
 *
 * @author harryzhu
 * @version V3.4
 * @date 2019/1/10
 */
public class BotUtil
{
    /**
     * 群机器人外网地址
     */
    private static final String BOT_OUTER_HOST = "https://qyapi.weixin.qq.com";

    /**
     * 群机器人内网地址
     */
    private static final String BOT_INNER_HOST = "http://in.qyapi.weixin.qq.com";

    public static void sendMsgToRobot(String url, String requestContent, Set<String> authors)
    {
        // 机器人外网地址转换为内网地址
        if (url.contains(BOT_OUTER_HOST))
        {
            url = url.replace(BOT_OUTER_HOST, BOT_INNER_HOST);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msgtype", "text");
        JSONObject textJson = new JSONObject();
        jsonObject.put("text", textJson);
        textJson.put("content", requestContent);
        JSONArray mentioned_list = new JSONArray();
        textJson.put("mentioned_list", mentioned_list);
        if (CollectionUtils.isNotEmpty(authors))
        {
            for (String author : authors)
            {
                mentioned_list.put(author);
            }
        }

        post(url, jsonObject.toString());
    }

    private static String post(String url, String requestContent)
    {
        Map<String, String> headerMap = Maps.newHashMap();
        headerMap.put("Content-Type", "application/json");
        return OkhttpUtils.INSTANCE.doHttpPost(url, requestContent, headerMap);
    }
}

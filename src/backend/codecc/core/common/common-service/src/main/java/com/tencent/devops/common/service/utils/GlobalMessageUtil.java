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

package com.tencent.devops.common.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.JsonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tencent.devops.common.constant.ComConstants.*;

/**
 * 国际化消息处理
 *
 * @version V1.0
 * @date 2019/7/17
 */
@Component
public class GlobalMessageUtil
{

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    private static Logger logger = LoggerFactory.getLogger(GlobalMessageUtil.class);


    /**
     * 通过redisKey获取国际化信息
     * 存 ==> key：String  value:Map<String, GlobalMessage>
     * 如：规则包、操作类型等方式
     *
     * @param redisKey key
     * @return 国际化信息
     */
    public Map<String, GlobalMessage> getGlobalMessageMap(String redisKey)
    {
        Map<String, GlobalMessage> messageCodeDetailMap = new HashMap<>();

        if (StringUtils.isNotBlank(redisKey))
        {
            Map<Object, Object> messageDetailMap = redisTemplate.opsForHash().entries(redisKey);

            if (MapUtils.isEmpty(messageDetailMap))
            {
                JedisConnectionFactory jedisConnectionFactory = (JedisConnectionFactory) redisTemplate.getConnectionFactory();
                logger.error("operation type map not initialized: {}, {}, {}, {}", redisKey,
                    jedisConnectionFactory.getHostName(),
                    jedisConnectionFactory.getPort(),
                    jedisConnectionFactory.getDatabase());
                return new HashMap<>();
            }
            for (Map.Entry<Object, Object> entry : messageDetailMap.entrySet())
            {
                String messageDetailStr = (String) entry.getValue();
                GlobalMessage globalMessage;
                try
                {
                    globalMessage = objectMapper.readValue(messageDetailStr, GlobalMessage.class);
                }
                catch (IOException e)
                {
                    logger.error("operation type message deserialize fail! key: {}", entry.getKey());
                    continue;
                }
                messageCodeDetailMap.put((String) entry.getKey(), globalMessage);
            }
        }

        return messageCodeDetailMap;
    }


    /**
     * 通过redisKey列表获取国际化信息
     * 存 ==> key：String  value:GlobalMessage
     * 如： 响应码存储方式
     *
     * @param keyList
     * @return
     */
    public Map<String, GlobalMessage> getGlobalByList(List<String> keyList)
    {
        Map<String, GlobalMessage> message = new HashMap<>();
        if (CollectionUtils.isNotEmpty(keyList))
        {
            for (String key : keyList)
            {
                String operMsgStr = redisTemplate.opsForValue().get(key);

                if (StringUtils.isBlank(operMsgStr)) {
                    continue;
                }

                GlobalMessage operMsgDetail;
                try
                {
                    operMsgDetail = objectMapper.readValue(operMsgStr, GlobalMessage.class);
                    message.put(key, operMsgDetail);
                }
                catch (Exception e)
                {
                    logger.error("operation history message deserialize fail!: {}, {}", key, operMsgStr);
                    throw new CodeCCException(CommonMessageCode.UTIL_EXECUTE_FAIL);
                }
            }
        }
        return message;
    }


    /**
     * 通过redisKey列表获取国际化信息
     * 存 ==> key：String  value:GlobalMessage
     * 如： 响应码存储方式
     *
     * @param redisKey
     * @return
     */
    private GlobalMessage getGlobalMessage(String redisKey)
    {
        String message = (String) redisTemplate.opsForValue().get(redisKey);
        try
        {
            return objectMapper.readValue(message, GlobalMessage.class);
        }
        catch (IOException e)
        {
            logger.error("operation history message deserialize fail!");
            throw new CodeCCException(CommonMessageCode.UTIL_EXECUTE_FAIL);
        }
    }


    /**
     * 根据国际化获取相应的消息
     *
     * @param globalMessage 国际化bean
     * @return 国际化信息
     */
    public String getMessageByLocale(GlobalMessage globalMessage)
    {
        return getMessageByLocale(globalMessage, getLocalLan());
    }


    /**
     * 根据国际化获取相应的消息
     *
     * @param globalMessage 国际化bean
     * @param locale        语言
     * @return 国际化信息
     */
    public String getMessageByLocale(GlobalMessage globalMessage, String locale)
    {
        if(Objects.isNull(globalMessage)){
            return STRING_TIPS;
        }
        // 后期加上繁体
        if (ZH_CN.equalsIgnoreCase(locale))
        {
            return globalMessage.getMessageZhCn();
        }
        else
        {
            return globalMessage.getMessageEn();
        }
    }


    /**
     * 从cookie中获取当前的语言
     * @return
     */
    public String getLocalLan() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (null != attributes) {
            Cookie[] cookies = attributes.getRequest().getCookies();
            if (null != cookies) {
                for (Cookie cookie : cookies) {
                    if (BLUEKING_LANGUAGE.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
        }

        return ZH_CN;
    }



}

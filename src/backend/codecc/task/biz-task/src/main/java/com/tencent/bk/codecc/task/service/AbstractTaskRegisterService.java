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

package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.MD5Utils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 任务注册抽象类
 *
 * @version V1.0
 * @date 2019/5/6
 */
public abstract class AbstractTaskRegisterService implements TaskRegisterService
{
    private static Logger logger = LoggerFactory.getLogger(AbstractTaskRegisterService.class);

    @Autowired
    protected TaskRepository taskRepository;

    @Autowired
    @Qualifier("redisTemplate")
    protected RedisTemplate redisTemplate;


    @Override
    public Boolean checkeIsStreamRegistered(String nameEn)
    {
        return taskRepository.existsByNameEn(nameEn);
    }

    /**
     * 创建代码扫描任务
     *
     * @param taskInfoEntity
     * @param userName
     */
    protected TaskInfoEntity createTask(TaskInfoEntity taskInfoEntity, String userName)
    {
        long currentTime = System.currentTimeMillis();
        taskInfoEntity.setCreatedBy(userName);
        taskInfoEntity.setCreatedDate(currentTime);
        taskInfoEntity.setUpdatedBy(userName);
        taskInfoEntity.setUpdatedDate(currentTime);
        //设置初始项目接口人及项目成员为自己
        List<String> users = new ArrayList<String>()
        {{
            add(userName);
        }};
        taskInfoEntity.setTaskOwner(users);
        taskInfoEntity.setTaskMember(users);

        //获取taskId主键
        long taskId = redisTemplate.opsForValue().increment(RedisKeyConstants.CODECC_TASK_ID, 1L);
        taskInfoEntity.setTaskId(taskId);


        //保存项目信息
        TaskInfoEntity taskInfoResult = taskRepository.save(taskInfoEntity);
        logger.info("save task info successfully! task id: {}, entity id: {}", taskId, taskInfoResult.getEntityId());
        return taskInfoResult;

    }


    /**
     * 获取任务英文名
     *
     * @param projectId
     * @param projectName
     * @return
     */
    protected String getTaskStreamName(String projectId, String projectName)
    {
        String md5Str = MD5Utils.getMD5(String.format("%s%s", projectId, projectName));
        return ComConstants.ENNAME_PREFIX + "_" + md5Str;
    }


    /**
     * 为了防止蓝盾平台调用创建任务接口添加任务校验中文名称失败，这里对中文名按照校验的正则表达式做一次处理
     *
     * @param cnName
     * @return
     */
    protected String handleCnName(String cnName)
    {
        if (StringUtils.isEmpty(cnName))
        {
            logger.error("cn name is empty!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{cnName}, null);
        }
        StringBuffer a = new StringBuffer(cnName);
        for (int i = 0; i < a.length(); i++)
        {
            String tmpStr = a.substring(i, i + 1);
            if (!regexMatch("[a-zA-Z0-9_\\u4e00-\\u9fa5]", tmpStr))
            {//单个字符匹配不上用下划线代替
                a.replace(i, i + 1, "_");
            }
        }
        //长度限制50个字符以内
        if (a.length() > 50)
        {
            return a.substring(0, 50);
        }
        return a.toString();
    }


    /**
     * 正则匹配,true表示匹配成功，false表示匹配失败
     *
     * @param regex
     * @param sourceText
     * @return
     */
    private boolean regexMatch(String regex, String sourceText)
    {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(sourceText);
        return matcher.matches();
    }

}

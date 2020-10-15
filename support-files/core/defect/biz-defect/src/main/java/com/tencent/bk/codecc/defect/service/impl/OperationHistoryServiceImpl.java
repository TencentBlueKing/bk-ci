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

package com.tencent.bk.codecc.defect.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.codecc.defect.dao.mongorepository.OperationHistoryRepository;
import com.tencent.bk.codecc.defect.model.OperationHistoryEntity;
import com.tencent.bk.codecc.defect.service.OperationHistoryService;
import com.tencent.bk.codecc.defect.vo.OperationHistoryVO;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.service.utils.GlobalMessageUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.RedisKeyConstants.GLOBAL_PREFIX_OPERATION_TYPE;
import static com.tencent.devops.common.constant.RedisKeyConstants.PREFIX_OPERATION_HISTORY_MSG;

/**
 * 操作记录历史服务实现类
 *
 * @version V1.0
 * @date 2019/6/18
 */
@Service
public class OperationHistoryServiceImpl implements OperationHistoryService
{
    private static Logger logger = LoggerFactory.getLogger(OperationHistoryServiceImpl.class);

    @Autowired
    private OperationHistoryRepository operationHistoryRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GlobalMessageUtil globalMessageUtil;

    @Override
    public List<OperationHistoryVO> getOperHisByTaskIdAndFuncId(long taskId, String toolName, List<String> funcId)
    {
        //1.获取操作记录消息的国际化信息
        Map<String, GlobalMessage> operMsgDetail = globalMessageUtil.getGlobalByList(convertionKey(funcId));

        //2.获取操作类型的国际化信息
        Map<String, GlobalMessage> operTypeDetailMap = globalMessageUtil.getGlobalMessageMap(GLOBAL_PREFIX_OPERATION_TYPE);
        String locale = globalMessageUtil.getLocalLan();

        List<OperationHistoryEntity> operationHistoryEntityList =
                operationHistoryRepository.findByTaskIdAndFuncIdInOrderByTimeDesc(taskId, funcId);
        //根据工具过滤
        if (StringUtils.isNotBlank(toolName)) {
            operationHistoryEntityList = operationHistoryEntityList.stream()
                    .filter(operationHistoryEntity ->
                            toolName.equalsIgnoreCase(operationHistoryEntity.getToolName())
                    )
                    .collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(operationHistoryEntityList)) {
            return new ArrayList<>();
        }
        return operationHistoryEntityList.stream()
            .map(operationHistoryEntity -> {
                OperationHistoryVO operationHistoryVO = new OperationHistoryVO();
                operationHistoryVO.setTaskId(operationHistoryEntity.getTaskId());
                operationHistoryVO.setFuncId(operationHistoryEntity.getFuncId());
                operationHistoryVO.setOperType(operationHistoryEntity.getOperType());
                operationHistoryVO.setToolName(operationHistoryEntity.getToolName());
                operationHistoryVO.setOperator(operationHistoryEntity.getOperator());
                operationHistoryVO.setOperTypeName(globalMessageUtil.getMessageByLocale(
                    operTypeDetailMap.get(operationHistoryEntity.getOperType()),
                    locale
                ));
                String str = String.format("%s%s", PREFIX_OPERATION_HISTORY_MSG, operationHistoryEntity.getFuncId());
                operationHistoryVO.setOperMsg(
                    MessageFormat.format(globalMessageUtil.getMessageByLocale(operMsgDetail.get(str), locale),
                        (Object[]) operationHistoryEntity.getParamArray()));
                operationHistoryVO.setTime(operationHistoryEntity.getTime());
                return operationHistoryVO;
            }).collect(Collectors.toList());
    }


    /**
     * 获取操作记录消息国际化信息
     *
     * @param funcIdList
     * @return
     */
    private List<String> convertionKey(List<String> funcIdList)
    {
        if (CollectionUtils.isNotEmpty(funcIdList))
        {
            return funcIdList.stream()
                    .map(funId -> String.format("%s%s", PREFIX_OPERATION_HISTORY_MSG, funId))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }


}

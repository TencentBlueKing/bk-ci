/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.GetFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonAuthorTransVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.tencent.devops.common.constant.ComConstants.AUTHOR_TRANSFER;
import static com.tencent.devops.common.constant.ComConstants.FUNC_DEFECT_MANAGE;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_MULTITOOL_HANDLE;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_MULTITOOL_AUTHOR_TRANS;

/**
 * 告警管理抽象类
 *
 * @version V1.0
 * @date 2019/5/28
 */
public abstract class AbstractQueryWarningBizService implements IQueryWarningBizService
{
    private static Logger logger = LoggerFactory.getLogger(AbstractQueryWarningBizService.class);

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Override
    public CommonDefectQueryRspVO processGetFileContentSegmentRequest(long taskId, GetFileContentSegmentReqVO reqModel)
    {
        return new CommonDefectQueryRspVO();
    }

    @Override
    @OperationHistory(funcId = FUNC_DEFECT_MANAGE, operType = AUTHOR_TRANSFER)
    public Boolean authorTransfer(long taskId, CommonAuthorTransVO commonAuthorTransVO)
    {
        authorTransCheckParam(commonAuthorTransVO);
        commonAuthorTransVO.setTaskId(taskId);
        logger.info("ready to send author transfer msg");
        rabbitTemplate.convertAndSend(EXCHANGE_MULTITOOL_HANDLE, ROUTE_MULTITOOL_AUTHOR_TRANS, commonAuthorTransVO);
        return true;
    }

    private void authorTransCheckParam(CommonAuthorTransVO commonAuthorTransVO)
    {
        List<String> sourceAuthor = commonAuthorTransVO.getSourceAuthor();
        List<String> targetAuthor = commonAuthorTransVO.getTargetAuthor();
        if (CollectionUtils.isEmpty(sourceAuthor) ||
                CollectionUtils.isEmpty(targetAuthor))
        {
            logger.error("source author or target author is empty!");
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{"原作者或者目标作者"}, null);
        }
        if (sourceAuthor.size() != targetAuthor.size())
        {
            logger.error("the size of source author and target author should be equivalent");
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, new String[]{"原作者或者目标作者"}, null);
        }
    }

    /**
     * 根据告警的开始行和结束行截取文件片段
     *
     * @param fileContent
     * @param beginLine
     * @param endLine
     * @param defectQueryRspVO
     * @return
     */
    protected String trimCodeSegment(String fileContent, int beginLine, int endLine, CommonDefectQueryRspVO defectQueryRspVO)
    {
        if (fileContent == null)
        {
            return "";
        }

        String[] lines = fileContent.split("\n");
        if (lines.length <= 2000)
        {
            defectQueryRspVO.setTrimBeginLine(1);
            return fileContent;
        }

        int trimBeginLine = 0;
        int trimEndLine = lines.length;
        int limitLines = 500;
        if (beginLine - limitLines > 0)
        {
            trimBeginLine = beginLine - limitLines;
        }

        if (endLine + limitLines < lines.length)
        {
            trimEndLine = endLine + limitLines;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = trimBeginLine; i < trimEndLine; i++)
        {
            builder.append(lines[i] + "\n");
        }
        defectQueryRspVO.setTrimBeginLine(trimBeginLine + 1);
        return builder.toString();
    }

}

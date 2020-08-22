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

package com.tencent.bk.codecc.task.dao;

import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 公共的dao
 *
 * @version V1.0
 * @date 2019/4/26
 */
@Repository
public class CommonDao
{
    private static Logger logger = LoggerFactory.getLogger(CommonDao.class);

    @Autowired
    private BaseDataRepository baseDataRepository;

    /**
     * 获取工具的排序
     *
     * @return
     */
    public String getToolOrder()
    {
        BaseDataEntity baseDataEntity = baseDataRepository.findFirstByParamType(ComConstants.KEY_TOOL_ORDER);
        final String toolIdsOrder;
        if (null != baseDataEntity)
        {
            toolIdsOrder = baseDataEntity.getParamValue();
        }
        else
        {
            logger.error("has not init tool order");
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }
        return toolIdsOrder;
    }

    /**
     * 获取语言的排序
     * @return
     */
    public String getLangOrder()
    {
        BaseDataEntity baseDataEntity = baseDataRepository.findFirstByParamType(ComConstants.KEY_LANG_ORDER);
        final String langOrder;
        if(null != baseDataEntity)
        {
            langOrder = baseDataEntity.getParamValue();
        }
        else
        {
            logger.error("has not init lang order");
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }
        return langOrder;
    }

}

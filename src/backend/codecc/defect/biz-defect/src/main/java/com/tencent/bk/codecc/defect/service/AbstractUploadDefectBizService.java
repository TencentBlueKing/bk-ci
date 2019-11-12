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

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.util.CompressionUtils;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 告警上报抽象类
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service("CCNUploadDefectBizService")
public abstract class AbstractUploadDefectBizService implements IBizService<UploadDefectVO>
{
    private static Logger logger = LoggerFactory.getLogger(AbstractUploadDefectBizService.class);

    /**
     * 工具侧是压缩后上报的，要先解压
     *
     * @param defectCompressStr
     * @return
     */
    protected String decompressDefects(String defectCompressStr)
    {
        logger.debug("defectCompressStr length: {}", defectCompressStr.length());
        String defectListJson;
        try
        {
            byte[] defectListDecompress = CompressionUtils.decompress(Base64.decodeBase64(defectCompressStr));
            defectListJson = new String(defectListDecompress != null ? defectListDecompress : new byte[0], StandardCharsets.UTF_8.name());
        }
        catch (IOException e)
        {
            logger.error("decompress defect list exception!", e);
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }
        logger.debug("defectDeCompressStr length: {}", defectListJson.length());

        return defectListJson;
    }
}

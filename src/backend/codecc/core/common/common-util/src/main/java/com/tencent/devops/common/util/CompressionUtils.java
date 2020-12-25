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

package com.tencent.devops.common.util;


import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressionUtils
{
    private static Logger logger = LoggerFactory.getLogger(CompressionUtils.class);

    public static byte[] compress(byte[] data)
    {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished())
        {
            // returns the generated code... index
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }

        try
        {
            outputStream.close();
        }
        catch (IOException e)
        {
            logger.error("compress exception-->", e);
        }
        return outputStream.toByteArray();
    }

    public static byte[] decompress(byte[] data)
    {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        try
        {
            while (!inflater.finished())
            {
                int count;
                count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
        }
        catch (IOException | DataFormatException e)
        {
            logger.error("decompress exception-->", e);
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }

        return outputStream.toByteArray();
    }

    /**
     * 压缩并采用Base64编码
     *
     * @param source
     * @return
     */
    public static String compressAndEncodeBase64(String source)
    {
        return Base64.encodeBase64String(compress(source.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Base64解码并解压缩
     *
     * @param source
     * @return
     */
    public static String decodeBase64AndDecompress(String source)
    {
        if (StringUtils.isEmpty(source))
        {
            return "";
        }

        String defectListJson;
        try
        {
            byte[] defectListDecompress = decompress(Base64.decodeBase64(source));
            defectListJson = new String(defectListDecompress != null ? defectListDecompress : new byte[0], StandardCharsets.UTF_8.name());
        }
        catch (IOException e)
        {
            logger.error("decompress defect list exception!", e);
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }

        return defectListJson;
    }
}

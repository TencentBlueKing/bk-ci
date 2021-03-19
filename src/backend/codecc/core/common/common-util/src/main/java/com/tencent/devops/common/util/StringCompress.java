package com.tencent.devops.common.util;


import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 字符串解压缩
 *
 * @version V1.0
 * @date 2019/12/4
 */
public class StringCompress
{
    private static Logger log = LoggerFactory.getLogger(StringCompress.class);

    /**
     * 压缩
     * @param str
     * @return
     */
    public static String compress(String str)
    {
        if (str == null || str.length() == 0)
        {
            return str;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
            GZIPOutputStream gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes());

            // gzip要在输出之前关闭，否则无法解压
            gzip.close();
            return out.toString("ISO-8859-1");
        }
        catch (IOException e)
        {
            log.error("compress exception-->", e);
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }
    }

    /**
     * 解压缩
     * @param str
     * @return
     */
    public static String uncompress(String str)
    {
        if (str == null || str.length() == 0)
        {
            return str;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes("ISO-8859-1"));
            GZIPInputStream gunzip = new GZIPInputStream(in))
        {
            byte[] buffer = new byte[256];
            int n;
            while ((n = gunzip.read(buffer)) >= 0)
            {
                out.write(buffer, 0, n);
            }
            return out.toString();
        }
        catch (IOException e)
        {
            log.error("uncompress exception-->", e);
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }

    }
}

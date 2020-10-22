package com.tencent.bk.codecc.klocwork.utils;

import com.tencent.bk.codecc.klocwork.constant.KlocworkMessageCode;
import com.tencent.devops.common.api.exception.CodeCCException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.*;

/**
 * klocwork的token工具类，用于读取platform对应的ltoken
 *
 * @version V2.0
 * @date 2019/11/15
 */
@Slf4j
public class LTokenUtil
{
    /**
     * 根据ip,port,user读取对应的ltoken
     * klocwork生成的ltkoen文件格式如下：
     * ip1;port1;username1;54b0c6a983b4126230eb2203egfd1e54dfbbc745c779852cea4f09011e078386
     * ip2;port2;username2;54b0c6a983b4126230eb2203egfd1e54dfbbc745c779852cea4f09011e078386
     *
     * @param ip
     * @param port
     * @param user
     * @return
     */
    public static String readLToken(String ip, String port, String user, String ltokenPath)
    {
        String ltoken = null;
        File file = new File(ltokenPath);
        BufferedReader reader = null;
        if (file.exists())
        {
            try
            {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                String ltokenStr;
                while ((ltokenStr = reader.readLine()) != null)
                {
                    String[] tokenParts = ltokenStr.split(";");
                    if (tokenParts.length == 4 && ip.equals(tokenParts[0]) && String.valueOf(port).equals(tokenParts[1]) && user.equals(tokenParts[2]))
                    {
                        ltoken = tokenParts[3];
                        break;
                    }
                }
            }
            catch (Exception e)
            {
                log.error("ERROR!!!", e);
            }
            finally
            {
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (IOException e)
                    {
                        log.error("ERROR!!!", e);
                    }
                }
            }
        }
        if (StringUtils.isEmpty(ltoken))
        {
            log.error("read klocwork ltoken fail. ip:{}, port:{}, user:{}", ip, port, user);
            throw new CodeCCException(KlocworkMessageCode.READ_LTOKEN_FAIL, new String[]{"read klocwork ltoken fail."}, null);
        }
        return ltoken;
    }
}

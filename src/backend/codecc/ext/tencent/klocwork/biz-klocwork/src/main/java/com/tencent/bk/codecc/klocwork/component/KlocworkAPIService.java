package com.tencent.bk.codecc.klocwork.component;

import com.tencent.bk.codecc.klocwork.constant.KlocworkMessageCode;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.AES128Endecryptor;
import com.tencent.devops.common.util.CompressionUtils;
import com.tencent.devops.common.web.RpcClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * klocwork api接口调用客户端
 *
 * @version V2.0
 * @date 2019/11/15
 */
@Component
@Slf4j
public class KlocworkAPIService
{
    private static Map<String, PlatformVO> PLATFORM_MAP = new HashedMap();

    // 允许注册新项目的示例
    private static Map<String, PlatformVO> ALLOW_REGISTER_PLATFORM_MAP = new HashedMap();

//    @Value("${platform.ltoken.path}")
//    private String ltokenPath;
//
    @Value("${platform.server.port}")
    private String serverPort;

    @Value("${security.dataTransferKey}")
    private String dataTransferKey;

    @Value("${security.dataTransferKeyIV}")
    private String dataTransferKeyIV;

    /**
     * 根据IP获取对应实例
     *
     * @param ip
     * @return
     */
    public static PlatformVO getInst(String ip)
    {
        PlatformVO platformVO = PLATFORM_MAP.get(ip);
        if (platformVO == null)
        {
            log.error("get klocwork platform instance by {} is null!", ip);
            throw new CodeCCException(KlocworkMessageCode.GET_KW_PLATFORM_INST_FAIL);
        }
        return platformVO;
    }

    public static Map<String, PlatformVO> getAllPlatformInst()
    {
        return PLATFORM_MAP;
    }

    public static Map<String, PlatformVO> getAllowRegisterPlatformInst()
    {
        return ALLOW_REGISTER_PLATFORM_MAP;
    }

    public static void initAllPlatform(List<PlatformVO> platformList)
    {
        if (CollectionUtils.isNotEmpty(platformList))
        {
            for (PlatformVO platform : platformList)
            {
                PLATFORM_MAP.put(platform.getIp(), platform);
                if (ComConstants.Status.DISABLE.value() != platform.getStatus())
                {
                    ALLOW_REGISTER_PLATFORM_MAP.put(platform.getIp(), platform);
                }
            }
        }
    }

    protected String doPost(String reqContent, String platformIp, String port)
    {
        String urlStr = String.format("http://%s:%s/review/api", platformIp, port);
        InputStream stream = null;
        HttpURLConnection urlConnection = null;
        OutputStreamWriter wr = null;
        BufferedReader reader = null;
        try
        {
            URL url = new URL(urlStr);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);

            urlConnection.connect();

            wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write(reqContent);
            wr.flush();

            stream = urlConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"), 8);

            StringBuffer rspContent = new StringBuffer();
            String eachLine;
            while ((eachLine = reader.readLine()) != null)
            {
                rspContent.append(eachLine).append("\n");
            }
            return rspContent.toString();
        }
        catch (IOException e)
        {
            log.error("Request klocwork[{}] exception", platformIp, e);
            throw new CodeCCException(KlocworkMessageCode.REQUEST_API_EXCEPTION, new String[]{"请求klocwork服务失败!"}, null);
        }
        finally
        {
            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
            try
            {
                if (stream != null)
                {
                    stream.close();
                }
                if (wr != null)
                {
                    wr.close();
                }
                if (reader != null)
                {
                    reader.close();
                }
            }
            catch (IOException e)
            {
                log.error("Request klocwork exception", e);
                throw new CodeCCException(KlocworkMessageCode.REQUEST_API_EXCEPTION, new String[]{"请求klocwork服务失败!"}, null);
            }
        }
    }

    /**
     * 查询告警
     * @param streamName
     * @param platformVO
     * @return
     */
    public String searchDefects(String streamName, PlatformVO platformVO)
    {
//        String ltoken = LTokenUtil.readLToken(platformIp, port, user, ltokenPath);
//        log.info("{}:{}", user, ltoken);
        StringBuffer sb = new StringBuffer();
        try
        {
            sb.append(URLEncoder.encode("action", "UTF-8")).append("=").append(URLEncoder.encode("search", "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("user", "UTF-8")).append("=").append(URLEncoder.encode(platformVO.getUserName(), "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("ltoken", "UTF-8")).append("=").append(URLEncoder.encode(platformVO.getToken(), "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("project", "UTF-8")).append("=").append(URLEncoder.encode(streamName, "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("query", "UTF-8")).append("=").append(URLEncoder.encode("state:+New,+Fixed,+Existing", "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            log.error("search klocwork defects exception", e);
            throw new CodeCCException(KlocworkMessageCode.REQUEST_API_EXCEPTION, new String[]{"search klocwork defects exception"}, null);
        }

        String responseMsg = doPost(sb.toString(), platformVO.getIp(), platformVO.getPort());

        String responseJsonArr = convertToJSONArray(responseMsg);
        return responseJsonArr;
    }

    /**
     * 获取构建列表
     * @param streamName
     * @param platformVO
     * @return
     */
    public JSONArray getBuilds(String streamName, PlatformVO platformVO)
    {
//        String ltoken = LTokenUtil.readLToken(platformIp, port, user, ltokenPath);
//        log.info("%s:%s", user, ltoken);
        StringBuffer sb = new StringBuffer();
        try
        {
            sb.append(URLEncoder.encode("action", "UTF-8")).append("=").append(URLEncoder.encode("builds", "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("user", "UTF-8")).append("=").append(URLEncoder.encode(platformVO.getUserName(), "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("ltoken", "UTF-8")).append("=").append(URLEncoder.encode(platformVO.getToken(), "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("project", "UTF-8")).append("=").append(URLEncoder.encode(streamName, "UTF-8"))
                    .append("&");
        }
        catch (UnsupportedEncodingException e)
        {
            log.error("get klocwork builds exception", e);
            throw new CodeCCException(KlocworkMessageCode.REQUEST_API_EXCEPTION, new String[]{"get klocwork builds exception"}, null);
        }

        String responseMsg = doPost(sb.toString(), platformVO.getIp(), platformVO.getPort());
        String responseJsonArr = convertToJSONArray(responseMsg);
        return new JSONArray(responseJsonArr);
    }

    private String convertToJSONArray(String responseMsg)
    {
        responseMsg = responseMsg.replaceAll("}\n", "},");
        if (responseMsg.lastIndexOf(",") != -1)
        {
            responseMsg = responseMsg.substring(0, responseMsg.lastIndexOf(","));
        }
        responseMsg = "[" + responseMsg + "]";
        return responseMsg;
    }

    /**
     * 打开/关闭klocwork规则
     *
     * @param streamName
     * @param platformVO
     * @param checker
     * @param enabledFlag true表示打开规则，false表示关闭规则
     * @return
     */
    public String updateCheckers(String streamName, PlatformVO platformVO, String checker, boolean enabledFlag)
    {
//        String ltoken = LTokenUtil.readLToken(platformIp, port, user, ltokenPath);
//        log.info("%s:%s", user, ltoken);
        StringBuffer sb = new StringBuffer();
        try
        {
            sb.append(URLEncoder.encode("action", "UTF-8")).append("=").append(URLEncoder.encode("update_defect_type", "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("user", "UTF-8")).append("=").append(URLEncoder.encode(platformVO.getUserName(), "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("ltoken", "UTF-8")).append("=").append(URLEncoder.encode(platformVO.getToken(), "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("project", "UTF-8")).append("=").append(URLEncoder.encode(streamName, "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("code", "UTF-8")).append("=").append(URLEncoder.encode(checker, "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("enabled", "UTF-8")).append("=").append(URLEncoder.encode(String.valueOf(enabledFlag), "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            log.error("updateCheckers exception", e);
            throw new CodeCCException(KlocworkMessageCode.REQUEST_API_EXCEPTION, new String[]{"updateCheckers exception"}, null);
        }

        /**
         * status执行状态	1表示成功，0表示失败
         * data	项目编号	新建项目编号
         * info	错误信息	如果status为0，则该字段表示错误信息
         */
        String responseMsg = doPost(sb.toString(), platformVO.getIp(), platformVO.getPort());

        return responseMsg;
    }

    /**
     * 新建klocwork项目
     *
     * @param platformIp
     * @param port
     * @param streamName
     * @return
     */
    public void createKWProject(String platformIp, String port, String streamName)
    {
        String kw_host = String.format("%s:%s", platformIp, port);
        String serverURL = String.format("http://%s:%s/", platformIp, serverPort);
        Object[] params = new Object[]{kw_host, streamName};
        RpcClient<Boolean> rpcClient = new RpcClient();
        Boolean response = rpcClient.doRequest(serverURL, "create_kw_project", params);
        if (response == null || !response)
        {
            log.error("create klocwork project fail! kw_host:{}, streamName:{}", kw_host, streamName);
            throw new CodeCCException(KlocworkMessageCode.REGISTER_PROJ_FAIL, new String[]{"create klocwork project fail!"}, null);
        }
    }

    /**
     * 获取文件内容
     * @param platformIp
     * @param streamName
     * @param build
     * @param filePath
     */
    public String getFileContent(String platformIp, String streamName, String build, String filePath)
    {
        String serverURL = String.format("http://%s:%s/", platformIp, serverPort);
        Object[] params = new Object[]{streamName, build, filePath};
        RpcClient<byte[]> rpcClient = new RpcClient();
        byte[] response = rpcClient.doRequest(serverURL, "get_code_content", params);

        if (response == null || response.length == 0)
        {
            log.error("get klocwork file contents fail! kw_host:{}, streamName:{}, build:{}, fileName:{}", platformIp, streamName, build, filePath);
            throw new CodeCCException(KlocworkMessageCode.REGISTER_PROJ_FAIL, new String[]{"get klocwork file contents fail!"}, null);
        }
        byte[] decompress = AES128Endecryptor.decryptAndReturnByte(dataTransferKey, dataTransferKeyIV, response);
        String fileContent;
        try
        {
            fileContent = new String(CompressionUtils.decompress(Base64.decodeBase64(decompress)), StandardCharsets.UTF_8.name());
        }
        catch (UnsupportedEncodingException e)
        {
            log.error("get klocwork file contents fail! kw_host:{}, streamName:{}, build:{}, fileName:{}", platformIp, streamName, build, filePath, e);
            throw new CodeCCException(KlocworkMessageCode.REGISTER_PROJ_FAIL, new String[]{"get klocwork file contents fail!"}, null);
        }

        return fileContent;
    }
}

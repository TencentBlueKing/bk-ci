package com.tencent.bk.devops.atom.utils.http;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * http请求工具类
 */
public class OkHttpUtils {

    private final static String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

    private final static Logger logger = LoggerFactory.getLogger(OkHttpUtils.class);


    private static OkHttpClient createClient(long connectTimeout, long writeTimeout, long readTimeout) {
        long finalConnectTimeout = 5L;
        long finalWriteTimeout = 60L;
        long finalReadTimeout = 60L;
        OkHttpClient.Builder builder = new okhttp3.OkHttpClient.Builder();
        if (connectTimeout > 0)
            finalConnectTimeout = connectTimeout;
        if (writeTimeout > 0)
            finalWriteTimeout = writeTimeout;
        if (readTimeout > 0)
            finalReadTimeout = readTimeout;
        builder.writeTimeout(finalConnectTimeout, TimeUnit.SECONDS);
        builder.writeTimeout(finalWriteTimeout, TimeUnit.SECONDS);
        builder.readTimeout(finalReadTimeout, TimeUnit.SECONDS);
        return builder.build();
    }


    private static Request.Builder getBuilder(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        if (null != headers) {
            builder.headers(Headers.of(headers));
        }
        return builder;
    }

    /**
     * http get方式请求，返回json格式响应报文
     *
     * @param url 请求路径
     * @return json格式响应报文
     */
    public static String doGet(String url) {
        return doGet(url, null);
    }

    /**
     * http get方式请求，返回json格式响应报文
     *
     * @param url            请求路径
     * @param connectTimeout 连接超时时间
     * @param writeTimeout   写超时时间
     * @param readTimeout    读超时时间
     * @return json格式响应报文
     */
    public static String doGet(String url, long connectTimeout, long writeTimeout, long readTimeout) {
        return doGet(url, null, connectTimeout, writeTimeout, readTimeout);
    }

    /**
     * http get方式请求，返回json格式响应报文
     *
     * @param url     请求路径
     * @param headers 请求头
     * @return json格式响应报文
     */
    public static String doGet(String url, Map<String, String> headers) {
        return doGet(url, headers, -1, -1, -1);
    }

    /**
     * http get方式请求，返回json格式响应报文
     *
     * @param url            请求路径
     * @param headers        请求头
     * @param connectTimeout 连接超时时间
     * @param writeTimeout   写超时时间
     * @param readTimeout    读超时时间
     * @return json格式响应报文
     */
    public static String doGet(String url, Map<String, String> headers, long connectTimeout, long writeTimeout, long readTimeout) {
        Request.Builder builder = getBuilder(url, headers);
        Request request = builder.get().build();
        return doHttp(request, connectTimeout, writeTimeout, readTimeout);
    }

    /**
     * http post方式请求，返回json格式响应报文
     *
     * @param url       请求路径
     * @param jsonParam json格式参数
     * @return json格式响应报文
     */
    public static String doPost(String url, String jsonParam) {
        return doPost(url, jsonParam, null);
    }

    /**
     * http post方式请求，返回json格式响应报文
     *
     * @param url            请求路径
     * @param jsonParam      json格式参数
     * @param connectTimeout 连接超时时间
     * @param writeTimeout   写超时时间
     * @param readTimeout    读超时时间
     * @return json格式响应报文
     */
    public static String doPost(String url, String jsonParam, long connectTimeout, long writeTimeout, long readTimeout) {
        return doPost(url, jsonParam, null, connectTimeout, writeTimeout, readTimeout);
    }

    /**
     * http post方式请求，返回json格式响应报文
     *
     * @param url       请求路径
     * @param jsonParam json格式参数
     * @param headers   请求头
     * @return json格式响应报文
     */
    public static String doPost(String url, String jsonParam, Map<String, String> headers) {
        return doPost(url, jsonParam, headers, -1, -1, -1);
    }

    /**
     * http post方式请求，返回json格式响应报文
     *
     * @param url            请求路径
     * @param jsonParam      json格式参数
     * @param headers        请求头
     * @param connectTimeout 连接超时时间
     * @param writeTimeout   写超时时间
     * @param readTimeout    读超时时间
     * @return json格式响应报文
     */
    public static String doPost(String url, String jsonParam, Map<String, String> headers, long connectTimeout, long writeTimeout, long readTimeout) {
        Request.Builder builder = getBuilder(url, headers);
        RequestBody body = RequestBody.create(MediaType.parse(CONTENT_TYPE_JSON), jsonParam);
        Request request = builder.post(body).build();
        return doHttp(request, connectTimeout, writeTimeout, readTimeout);
    }

    /**
     * http put方式请求，返回json格式响应报文
     *
     * @param url       请求路径
     * @param jsonParam json格式参数
     * @return json格式响应报文
     */
    public static String doPut(String url, String jsonParam) {
        return doPut(url, jsonParam, null);
    }

    /**
     * http put方式请求，返回json格式响应报文
     *
     * @param url            请求路径
     * @param jsonParam      json格式参数
     * @param connectTimeout 连接超时时间
     * @param writeTimeout   写超时时间
     * @param readTimeout    读超时时间
     * @return json格式响应报文
     */
    public static String doPut(String url, String jsonParam, long connectTimeout, long writeTimeout, long readTimeout) {
        return doPut(url, jsonParam, null, connectTimeout, writeTimeout, readTimeout);
    }

    /**
     * http put方式请求，返回json格式响应报文
     *
     * @param url       请求路径
     * @param jsonParam json格式参数
     * @param headers   请求头
     * @return json格式响应报文
     */
    public static String doPut(String url, String jsonParam, Map<String, String> headers) {
        return doPut(url, jsonParam, headers, -1, -1, -1);
    }

    /**
     * http put方式请求，返回json格式响应报文
     *
     * @param url            请求路径
     * @param jsonParam      json格式参数
     * @param headers        请求头
     * @param connectTimeout 连接超时时间
     * @param writeTimeout   写超时时间
     * @param readTimeout    读超时时间
     * @return json格式响应报文
     */
    public static String doPut(String url, String jsonParam, Map<String, String> headers, long connectTimeout, long writeTimeout, long readTimeout) {
        Request.Builder builder = getBuilder(url, headers);
        RequestBody body = RequestBody.create(MediaType.parse(CONTENT_TYPE_JSON), jsonParam);
        Request request = builder.put(body).build();
        return doHttp(request, connectTimeout, writeTimeout, readTimeout);
    }

    /**
     * http delete方式请求，返回json格式响应报文
     *
     * @param url 请求路径
     * @return json格式响应报文
     */
    public static String doDelete(String url) {
        return doDelete(url, null);
    }

    /**
     * http delete方式请求，返回json格式响应报文
     *
     * @param url            请求路径
     * @param connectTimeout 连接超时时间
     * @param writeTimeout   写超时时间
     * @param readTimeout    读超时时间
     * @return json格式响应报文
     */
    public static String doDelete(String url, long connectTimeout, long writeTimeout, long readTimeout) {
        return doDelete(url, null, connectTimeout, writeTimeout, readTimeout);
    }

    /**
     * http delete方式请求，返回json格式响应报文
     *
     * @param url     请求路径
     * @param headers 请求头
     * @return json格式响应报文
     */
    public static String doDelete(String url, Map<String, String> headers) {
        return doDelete(url, headers, -1, -1, -1);
    }

    /**
     * http delete方式请求，返回json格式响应报文
     *
     * @param url            请求路径
     * @param headers        请求头
     * @param connectTimeout 连接超时时间
     * @param writeTimeout   写超时时间
     * @param readTimeout    读超时时间
     * @return json格式响应报文
     */
    public static String doDelete(String url, Map<String, String> headers, long connectTimeout, long writeTimeout, long readTimeout) {
        Request.Builder builder = getBuilder(url, headers);
        Request request = builder.delete().build();
        return doHttp(request, connectTimeout, writeTimeout, readTimeout);
    }

    public static String doHttp(Request request, long connectTimeout, long writeTimeout, long readTimeout) {
        OkHttpClient httpClient = createClient(connectTimeout, writeTimeout, readTimeout);
        Response response = null;
        String responseContent = null;
        try {
            response = httpClient.newCall(request).execute();
            assert response.body() != null;
            responseContent = response.body().string();
        } catch (IOException e) {
            logger.error("http send  throw Exception", e);
        } finally {
            if (response != null) {
                assert response.body() != null;
                response.body().close();
            }
        }
        if (response != null && !response.isSuccessful()) {
            logger.error("Fail to request(" + request + ") with code " + response.code()
                    + " , message " + response.message() + " and response" + responseContent);
        }
        return responseContent;
    }

}

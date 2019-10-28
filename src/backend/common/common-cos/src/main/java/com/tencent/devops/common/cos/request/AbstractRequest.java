package com.tencent.devops.common.cos.request;

import com.tencent.devops.common.cos.COSClientConfig;
import com.tencent.devops.common.cos.model.enums.EnvEnum;
import com.tencent.devops.common.cos.util.ParamsUtil;
import com.tencent.devops.common.cos.model.enums.HttpMethodEnum;
import com.tencent.devops.common.cos.model.enums.SignTypeEnum;
import com.tencent.devops.common.cos.model.exception.COSException;
import com.tencent.devops.common.cos.util.EncodeUtil;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import net.sf.json.JSONObject;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by liangyuzhou on 2017/2/10.
 * Powered By Tencent
 */
public abstract class AbstractRequest implements IRequest {
    private final static String FORMAT_STRING_SEP = "\n";
    private final static String SIGN_ALGORITHM = "sha1";
    protected final static MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/xml");

    private final String bucketName;

    public AbstractRequest(final String bucketName) throws COSException {
        if (StringUtils.isEmpty(bucketName)) {
            throw new COSException("Invalid bucket name");
        }
        this.bucketName = bucketName;
    }

    public Request getRequest(final COSClientConfig cosClientConfig) throws COSException {
        final Pair<HttpMethodEnum, RequestBody> methodPair = getMethod();
        final Map<String, String> headerParam = getFinalHeaderParams(cosClientConfig);
        final Request.Builder builder = new Request.Builder()
                .headers(Headers.of(headerParam))
                .method(methodPair.getLeft().name(), methodPair.getRight());
        if (isNeedSign()) {
            String authorization;
            if (cosClientConfig.isFromSpm()) {
                authorization = getCosSign(cosClientConfig.getSpmBuId(), cosClientConfig.getSpmSecretKey(),
                        methodPair.getLeft().name(), getPath(), headerParam, getQueryParams());
            } else {
                authorization = getAuthorization(cosClientConfig);
            }

            if (getSignType().equals(SignTypeEnum.HEADER)) {
                builder.addHeader("Authorization", authorization);
                builder.url(getRequestUrl(cosClientConfig, null));
            } else {
                builder.url(getRequestUrl(cosClientConfig, authorization));
            }
        } else {
            builder.url(getRequestUrl(cosClientConfig, null));
        }
        return builder.build();
    }

    private String getRequestUrl(final COSClientConfig cosClientConfig, final String authorization) {
        StringBuilder sb = new StringBuilder();
        //sb.append(cosClientConfig.getEnv() == EnvEnum.IDC ? "https://" : "http://");
        sb.append("http://");
        sb.append(getBucketName());
        sb.append("-");
        sb.append(cosClientConfig.getAppId());
        sb.append(".");
        sb.append(cosClientConfig.getRegion());
        sb.append(".");
        sb.append(cosClientConfig.getEnv().getType());
        sb.append(getPath());
        final String queryParamString = ParamsUtil.getQueryParamString(getQueryParams());
        if (!StringUtils.isEmpty(queryParamString)) {
            sb.append("?");
            sb.append(queryParamString);
        }
        if (!StringUtils.isEmpty(authorization)) {
            if (!StringUtils.isEmpty(queryParamString)) {
                sb.append("&");
            } else {
                sb.append("?");
            }
            sb.append("sign=");
            sb.append(ParamsUtil.urlEncode(authorization));
        }
        return sb.toString();
    }

    public static String getPublicRequestUrl(final COSClientConfig cosClientConfig, final String bucketName, final String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(cosClientConfig.getEnv() == EnvEnum.IDC ? "https://" : "http://");
        sb.append(bucketName);
        sb.append("-");
        sb.append(cosClientConfig.getAppId());
        sb.append(".");
        sb.append(cosClientConfig.getRegion());
        sb.append(".");
        sb.append(cosClientConfig.getEnv().getType());
        sb.append(path);
        return sb.toString();
    }

    private String getAuthorization(final COSClientConfig cosClientConfig) throws COSException {
        // 获取各种要素
        Map<String, String> headerParams = getFinalHeaderParams(cosClientConfig);
        Map<String, String> queryParams = getQueryParams();
        // 处理SIGN KEY
        Instant now = Instant.now();
        long nowTimestamp = now.getEpochSecond() - (30 * 60);
        long expireTimestamp = nowTimestamp + getSignExpireSeconds();
        final String qKeyTime = String.format("%d;%d", nowTimestamp, expireTimestamp);
        String signKey;
        try {
            signKey = EncodeUtil.HmacSHA1Encrypt(qKeyTime, cosClientConfig.getSecretKey());
        } catch (Exception e) {
            throw new COSException("Encrypt sign key failure", e);
        }
        final String formatString = getMethod().getLeft().getType() +
                FORMAT_STRING_SEP +
                getPath() +
                FORMAT_STRING_SEP +
                ParamsUtil.getQueryParamSignString(queryParams) +
                FORMAT_STRING_SEP +
                ParamsUtil.getHeaderSignString(headerParams) +
                FORMAT_STRING_SEP;
        String sha1FormatString;
        try {
            sha1FormatString = EncodeUtil.SHA1Encrypt(formatString);
        } catch (Exception e) {
            throw new COSException("Encrypt format string failure", e);
        }
        final String stringToSign = SIGN_ALGORITHM +
                FORMAT_STRING_SEP +
                qKeyTime +
                FORMAT_STRING_SEP +
                sha1FormatString +
                FORMAT_STRING_SEP;
        String sign;
        try {
            sign = EncodeUtil.HmacSHA1Encrypt(stringToSign, signKey);
        } catch (Exception e) {
            throw new COSException("Encrypt sign failure", e);
        }
        // 获取q-header-list
        String qHeaderList;
        if (headerParams == null || headerParams.isEmpty()) {
            qHeaderList = "";
        } else {
            qHeaderList = ParamsUtil.getQueryParamKeyListString(headerParams);
        }
        String qUrlParamsList;
        if (queryParams == null || queryParams.isEmpty()) {
            qUrlParamsList = "";
        } else {
            qUrlParamsList = ParamsUtil.getQueryParamKeyListString(queryParams);
        }
        return String.format("q-sign-algorithm=%s&q-ak=%s&q-sign-time=%s&q-key-time=%s&q-header-list=%s&q-url-param-list=%s&q-signature=%s", SIGN_ALGORITHM, cosClientConfig.getSecretId(), qKeyTime, qKeyTime, qHeaderList, qUrlParamsList, sign);
    }

    public String getBucketName() {
        return bucketName;
    }

    private Map<String, String> getFinalHeaderParams(final COSClientConfig cosClientConfig) {
        Map<String, String> headerParams = getHeaderParams();
        if (headerParams == null) {
            headerParams = new HashMap<>();
        }
        headerParams.put("Host", String.format("%s-%d.%s.%s", getBucketName(), cosClientConfig.getAppId(), cosClientConfig.getRegion(), cosClientConfig.getEnv().getType()));
        return headerParams;
    }

    private String getCosSign(String spmAppId, String spmSecretKey, String method, String uri, Map<String, String> headerParam,
                              Map<String, String> param) throws COSException {
        String url = "http://spm.oa.com/cdntool/remote_auth.py";

        try {
            JSONObject requestJson = new JSONObject();
            requestJson.put("bu_id", spmAppId);
            requestJson.put("secret_key", spmSecretKey);
            requestJson.put("method", method);
            requestJson.put("uri", uri);
            requestJson.put("params", param);
            requestJson.put("headers", headerParam);

            String requestBody = requestJson.toString();

            Request request = new Request.Builder().url(url).post(RequestBody.create(MediaType.parse("application/json;charset=utf-8"), requestBody)).build();

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(5L, TimeUnit.SECONDS)
                    .readTimeout(60L, TimeUnit.SECONDS)
                    .writeTimeout(60L, TimeUnit.SECONDS)
                    .build();

            Call call = okHttpClient.newCall(request);

            Response response = call.execute();
            String responseBody = response.body().string();
            JSONObject responseJson = JSONObject.fromObject(responseBody);
            int code = responseJson.optInt("code");
            if (0 != code) {
                String msg = responseJson.optString("msg");
                throw new COSException("Get cos sign from spm failed, msg:" + msg);
            }
            return responseJson.optString("sign");
        } catch (Exception e) {
            throw new COSException("Get cos sign from spm failed.");
        }
    }

//    public static void main(String[] args) {
//        try {
//            getCosSign("45", "8af87dc9", "DELETE", "/hellocdn/ProxifierMac.dmg", new HashMap<String, String>(), new HashMap<String, String>());
//        } catch (COSException e) {
//            e.printStackTrace();
//        }
//    }
}

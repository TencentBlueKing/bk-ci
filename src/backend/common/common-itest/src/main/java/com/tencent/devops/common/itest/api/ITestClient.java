package com.tencent.devops.common.itest.api;

import com.tencent.devops.common.itest.api.pojo.ITestException;
import com.tencent.devops.common.itest.api.request.*;
import com.tencent.devops.common.itest.api.response.*;
import net.sf.cglib.beans.BeanMap;
import net.sf.json.JSONObject;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

public class ITestClient {

    private String apiUser;
    private String token;
    private String iTestAddr = "http://api.itest.oa.com/v2/api/";

    public ITestClient(String apiUser, String token) {
        this.apiUser = apiUser;
        this.token = token;
    }

    public ReviewCreateResponse createReview(ReviewCreateRequest request) throws IOException, ITestException {
        String url = "review/create";
        return post(url, request, ReviewCreateResponse.class);
    }

    public TaskCreateResponse createTask(TaskCreateRequest request) throws IOException, ITestException {
        String url = "task/create";
        return post(url, request, TaskCreateResponse.class);
    }

    public ProcessCreateResponse createProcess(ProcessCreateRequest request) throws IOException, ITestException {
        String url = "process/create";
        return post(url, request, ProcessCreateResponse.class);
    }

    public VersionGetResponse getVersion(VersionGetRequest request) throws IOException, ITestException {
        String url = "version/get";
        return get(url, request, VersionGetResponse.class);
    }

    public VersionGetVersionOnlyResponse getVersionOnly(VersionGetVersionOnlyRequest request) throws IOException, ITestException {
        String url = "version/get_version_only";
        return get(url, request, VersionGetVersionOnlyResponse.class);
    }

    public VersionGetBaselineByVersionResponse getVersionBaselineByVersion(VersionGetBaselineByVersionRequest request) throws IOException, ITestException {
        String url = "version/get_baseline_by_version";
        return get(url, request, VersionGetBaselineByVersionResponse.class);
    }

    public ProcessTestMasterResponse getProcessTestMaster(ProcessTestMasterRequest request) throws IOException, ITestException {
        String url = "process/test_master";
        return get(url, request, ProcessTestMasterResponse.class);
    }


    private <T extends BaseResponse> T post(final String url, final BaseRequest request, final Class<T> classOfResponse)
            throws ITestException, IOException {
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("api_user", apiUser);
        builder.add("token", token);
        BeanMap beanMap = BeanMap.create(request);
        for (Object key : beanMap.keySet()) {
            builder.add(key.toString(), (String) beanMap.get(key));
        }

        Request request1 = new Request.Builder().url(iTestAddr + url).post(builder.build()).build();
        return sendRequest(classOfResponse, request1);
    }

    private OkHttpClient client = new OkHttpClient.Builder().connectTimeout(5L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .writeTimeout(60L, TimeUnit.SECONDS)
            .build();

    private <T extends BaseResponse> T sendRequest(Class<T> classOfResponse, Request request1) throws IOException, ITestException {

        Call call = client.newCall(request1);
        Response response = call.execute();
        String responseStr = response.body().string();
        JSONObject jsonObject = JSONObject.fromObject(responseStr);
        int code = jsonObject.optInt("code");
        if (code == 200) {
            return (T) JSONObject.toBean(jsonObject, classOfResponse);
        } else {
            String msg = jsonObject.optString("message");
            throw new ITestException("Call itest failed, code:" + code + " and message:" + msg);
        }
    }

    private <T extends BaseResponse> T get(final String url, final BaseRequest request, final Class<T> classOfResponse)
            throws IOException, ITestException {

        StringBuilder queryParam = new StringBuilder("?api_user=" + apiUser + "&token=" + token);
        BeanMap beanMap = BeanMap.create(request);
        for (Object key : beanMap.keySet()) {
            queryParam.append("&").append(key.toString()).append("=").append(URLEncoder.encode((String) beanMap.get(key)));
        }
        Request itestRequest = new Request.Builder().url(iTestAddr + url + queryParam).get().build();
        return sendRequest(classOfResponse, itestRequest);
    }
}

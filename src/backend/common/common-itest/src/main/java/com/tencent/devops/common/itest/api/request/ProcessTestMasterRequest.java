package com.tencent.devops.common.itest.api.request;

public class ProcessTestMasterRequest extends BaseRequest {
    String project_id;      //  项目id

    public ProcessTestMasterRequest(String project_id) {
        this.project_id = project_id;
    }

    public String getProject_id() {
        return project_id;
    }

    public void setProject_id(String project_id) {
        this.project_id = project_id;
    }
}

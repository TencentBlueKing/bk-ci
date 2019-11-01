package com.tencent.devops.common.itest.api.request;

public class VersionGetRequest extends BaseRequest {
    String project_id;      //  项目id
    String is_complete;     //  版本是否关闭，0表示未关闭、1表示关闭

    public VersionGetRequest(String project_id,
                             String is_complete
    ) {
        this.project_id = project_id;
        this.is_complete = is_complete;
    }

    public String getProject_id() {
        return project_id;
    }

    public void setProject_id(String project_id) {
        this.project_id = project_id;
    }

    public String getIs_complete() {
        return is_complete;
    }

    public void setIs_complete(String is_complete) {
        this.is_complete = is_complete;
    }
}

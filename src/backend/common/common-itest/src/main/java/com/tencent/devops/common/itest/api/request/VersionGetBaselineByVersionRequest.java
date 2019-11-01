package com.tencent.devops.common.itest.api.request;

public class VersionGetBaselineByVersionRequest extends BaseRequest {

    String project_id;      //  项目id
    String version_id;      //  版本id
    String is_complete;     //  版本是否关闭，0表示未关闭、1表示关闭
    String is_idle;         //  基线是否空闲（可以提测），0表示所有、1表示空闲（可以提测）

    public VersionGetBaselineByVersionRequest(String project_id,
                                              String version_id,
                                              String is_complete,
                                              String is_idle
    ) {
        this.project_id = project_id;
        this.version_id = version_id;
        this.is_complete = is_complete;
        this.is_idle = is_idle;
    }

    public String getProject_id() {
        return project_id;
    }

    public void setProject_id(String project_id) {
        this.project_id = project_id;
    }

    public String getVersion_id() {
        return version_id;
    }

    public void setVersion_id(String version_id) {
        this.version_id = version_id;
    }

    public String getIs_complete() {
        return is_complete;
    }

    public void setIs_complete(String is_complete) {
        this.is_complete = is_complete;
    }

    public String getIs_idle() {
        return is_idle;
    }

    public void setIs_idle(String is_idle) {
        this.is_idle = is_idle;
    }
}

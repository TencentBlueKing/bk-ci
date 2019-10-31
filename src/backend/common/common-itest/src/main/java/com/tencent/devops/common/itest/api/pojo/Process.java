package com.tencent.devops.common.itest.api.pojo;

public class Process {
    private String id;
    private String project_id;
    private String version_id;
    private String baseline_id;
    private String test_description;
    private String test_type;
    private String version_type;
    private String test_master;
    private String create_user;
    private String create_time;
    private String state;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getBaseline_id() {
        return baseline_id;
    }

    public void setBaseline_id(String baseline_id) {
        this.baseline_id = baseline_id;
    }

    public String getTest_description() {
        return test_description;
    }

    public void setTest_description(String test_description) {
        this.test_description = test_description;
    }

    public String getTest_type() {
        return test_type;
    }

    public void setTest_type(String test_type) {
        this.test_type = test_type;
    }

    public String getVersion_type() {
        return version_type;
    }

    public void setVersion_type(String version_type) {
        this.version_type = version_type;
    }

    public String getTest_master() {
        return test_master;
    }

    public void setTest_master(String test_master) {
        this.test_master = test_master;
    }

    public String getCreate_user() {
        return create_user;
    }

    public void setCreate_user(String create_user) {
        this.create_user = create_user;
    }

    public String getCreate_time() {
        return create_time;
    }

    public void setCreate_time(String create_time) {
        this.create_time = create_time;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}

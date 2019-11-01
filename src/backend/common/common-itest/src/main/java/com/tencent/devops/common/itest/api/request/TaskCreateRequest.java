package com.tencent.devops.common.itest.api.request;

public class TaskCreateRequest extends BaseRequest {
    private String name;
    private String project_id;
    private String description;
    private String type_id;         //	任务类型id（"12"=>"功能用例设计","7"=>"功能测试执行","14"=>"专项用例设计","15"=>"专项测试执行","5"=>"用例评审","6"=>"工具开发","8"=>"文件检查"）
    private String create_user;     //	任务创建人
    private String parent_id;       //	父任务ID（如果是创建子任务，则该参数为必填项）
    private String current_user;    //	任务执行人(多个以';'号隔开，添加此参数即为分派任务)
    private String version_id;      //	项目版本id
    private String baseline_id;     //	项目基线id

    public TaskCreateRequest(String name,
                             String project_id,
                             String description,
                             String type_id,
                             String create_user,
                             String parent_id,
                             String current_user,
                             String version_id,
                             String baseline_id) {
        this.name = name;
        this.project_id = project_id;
        this.description = description;
        this.type_id = type_id;
        this.create_user = create_user;
        this.parent_id = parent_id;
        this.current_user = current_user;
        this.version_id = version_id;
        this.baseline_id = baseline_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProject_id() {
        return project_id;
    }

    public void setProject_id(String project_id) {
        this.project_id = project_id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType_id() {
        return type_id;
    }

    public void setType_id(String type_id) {
        this.type_id = type_id;
    }

    public String getCreate_user() {
        return create_user;
    }

    public void setCreate_user(String create_user) {
        this.create_user = create_user;
    }

    public String getParent_id() {
        return parent_id;
    }

    public void setParent_id(String parent_id) {
        this.parent_id = parent_id;
    }

    public String getCurrent_user() {
        return current_user;
    }

    public void setCurrent_user(String current_user) {
        this.current_user = current_user;
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
}

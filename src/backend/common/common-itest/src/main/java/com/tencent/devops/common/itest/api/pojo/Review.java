package com.tencent.devops.common.itest.api.pojo;

/**
 * 审核单，只有iTest上的几个固定的项目可以使用：'2254','2000','2563','2496','2597','2646','2515','2704'
 * 其他项目不开放
 */
public class Review {
    private Long id;
    private String project_id;
    private String version_commit_time;
    private String test_master;
    private String relate_process_id;
    private String create_user;
    private String create_time;
    private String state;
    private String version_desc;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProject_id() {
        return project_id;
    }

    public void setProject_id(String project_id) {
        this.project_id = project_id;
    }

    public String getVersion_commit_time() {
        return version_commit_time;
    }

    public void setVersion_commit_time(String version_commit_time) {
        this.version_commit_time = version_commit_time;
    }

    public String getTest_master() {
        return test_master;
    }

    public void setTest_master(String test_master) {
        this.test_master = test_master;
    }

    public String getRelate_process_id() {
        return relate_process_id;
    }

    public void setRelate_process_id(String relate_process_id) {
        this.relate_process_id = relate_process_id;
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

    public String getVersion_desc() {
        return version_desc;
    }

    public void setVersion_desc(String version_desc) {
        this.version_desc = version_desc;
    }
}

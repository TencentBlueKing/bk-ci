package com.tencent.devops.common.itest.api.request;

public class ProcessCreateRequest extends BaseRequest {
    String project_id;      //  项目id  是
    String create_user;     //  创建人   是
    String version_name;    //        版本名称  是
    String baseline_name;   //        基线名称  是
    String description;     //  转测单描述 是
    String version_type;    //        版本类型（默认值为new，其中：'new' => '新功能版本', 'urgent' => '紧急修复版本', 'back' => '回归版本'）     否
    String test_type;       //  测试类型（默认值为function test，其中：'function test' => '功能测试', 'compatibility test' => '兼容测试', 'server performance test' => '服务器性能测试', 'client performance test' => '客户端性能测试', 'network test' => '网络测试', 'protocol test' => '协议测试', 'international test' => '国际化测试', 'secure test' => '安全类测试', 'module test' => '组件测试'）；多个测试类型使用“;”分号隔开 否
    String test_master;     //  测试经理（默认值为项目的全部测试经理）；测试经理英文帐号之间使用“;”分号隔开   否
    String tapd_story;      //  tapd的id，多个之间使用“,”逗号隔开   否
    String tapd_task;       //  tapd的id，多个之间使用“,”逗号隔开   否
    String tapd_bug;        //  tapd的id，多个之间使用“,”逗号隔开   否
    String attachment;      //  附件，分拆成数组post过来 [{"name":"ftest1.txt","size":"1111","filepath":"http://file.ieg.local/download/cfs/ITEST/20140710/2000137359908582619.1404980904.txt"},{"name":"ftest2.txt","size":"2222","filepath":"http://file.ieg.local/download/cfs/ITEST/20140710/2000137359908582619.1404980904.txt"}]    否

    public ProcessCreateRequest(String project_id,
                                String create_user,
                                String version_name,
                                String baseline_name,
                                String description,
                                String version_type,
                                String test_type,
                                String test_master,
                                String tapd_story,
                                String tapd_task,
                                String tapd_bug,
                                String attachment
    ) {
        this.project_id = project_id;
        this.create_user = create_user;
        this.version_name = version_name;
        this.baseline_name = baseline_name;
        this.description = description;
        this.version_type = version_type;
        this.test_type = test_type;
        this.test_master = test_master;
        this.tapd_story = tapd_story;
        this.tapd_task = tapd_task;
        this.tapd_bug = tapd_bug;
        this.attachment = attachment;
    }

    public String getProject_id() {
        return project_id;
    }

    public void setProject_id(String project_id) {
        this.project_id = project_id;
    }

    public String getCreate_user() {
        return create_user;
    }

    public void setCreate_user(String create_user) {
        this.create_user = create_user;
    }

    public String getVersion_name() {
        return version_name;
    }

    public void setVersion_name(String version_name) {
        this.version_name = version_name;
    }

    public String getBaseline_name() {
        return baseline_name;
    }

    public void setBaseline_name(String baseline_name) {
        this.baseline_name = baseline_name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion_type() {
        return version_type;
    }

    public void setVersion_type(String version_type) {
        this.version_type = version_type;
    }

    public String getTest_type() {
        return test_type;
    }

    public void setTest_type(String test_type) {
        this.test_type = test_type;
    }

    public String getTest_master() {
        return test_master;
    }

    public void setTest_master(String test_master) {
        this.test_master = test_master;
    }

    public String getTapd_story() {
        return tapd_story;
    }

    public void setTapd_story(String tapd_story) {
        this.tapd_story = tapd_story;
    }

    public String getTapd_task() {
        return tapd_task;
    }

    public void setTapd_task(String tapd_task) {
        this.tapd_task = tapd_task;
    }

    public String getTapd_bug() {
        return tapd_bug;
    }

    public void setTapd_bug(String tapd_bug) {
        this.tapd_bug = tapd_bug;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }
}

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.itest.api.pojo;

public class Task {
    private String id;
    private String project_id;
    private String parent_id;
    private String level;
    private String version_id;
    private String baseline_id;
    private String module_id;
    private String flow_type;
    private String is_version_test;
    private String type_id;
    private String type_parent_id;
    private String name;
    private String description;
    private String create_user;
    private String create_time;
    private String update_time;
    private String current_user;
    private String check_user;
    private String state;
    private String expect_begin_time;
    private String expect_end_time;
    private String expect_hours;
    private String complete_time;
    private String real_hours;
    private String role;
    private String progress;
    private String tapd_id;
    private String tapd_type;
    private String ttms_id;
    private String case_total;
    private String pass_case_total;
    private String nopass_case_total;
    private String unable_case_total;
    private String bug_total;
    private String bug_serious_total;
    private String compare_file_path;
    private String design_id;
    private String all_parent_id;
    private String attachment_total;
    private String task_user;
    private String is_smart_design;
    private String source;
    private String enforce_case;
    private String testing_expenses;

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

    public String getParent_id() {
        return parent_id;
    }

    public void setParent_id(String parent_id) {
        this.parent_id = parent_id;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
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

    public String getModule_id() {
        return module_id;
    }

    public void setModule_id(String module_id) {
        this.module_id = module_id;
    }

    public String getFlow_type() {
        return flow_type;
    }

    public void setFlow_type(String flow_type) {
        this.flow_type = flow_type;
    }

    public String getIs_version_test() {
        return is_version_test;
    }

    public void setIs_version_test(String is_version_test) {
        this.is_version_test = is_version_test;
    }

    public String getType_id() {
        return type_id;
    }

    public void setType_id(String type_id) {
        this.type_id = type_id;
    }

    public String getType_parent_id() {
        return type_parent_id;
    }

    public void setType_parent_id(String type_parent_id) {
        this.type_parent_id = type_parent_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(String update_time) {
        this.update_time = update_time;
    }

    public String getCurrent_user() {
        return current_user;
    }

    public void setCurrent_user(String current_user) {
        this.current_user = current_user;
    }

    public String getCheck_user() {
        return check_user;
    }

    public void setCheck_user(String check_user) {
        this.check_user = check_user;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getExpect_begin_time() {
        return expect_begin_time;
    }

    public void setExpect_begin_time(String expect_begin_time) {
        this.expect_begin_time = expect_begin_time;
    }

    public String getExpect_end_time() {
        return expect_end_time;
    }

    public void setExpect_end_time(String expect_end_time) {
        this.expect_end_time = expect_end_time;
    }

    public String getExpect_hours() {
        return expect_hours;
    }

    public void setExpect_hours(String expect_hours) {
        this.expect_hours = expect_hours;
    }

    public String getComplete_time() {
        return complete_time;
    }

    public void setComplete_time(String complete_time) {
        this.complete_time = complete_time;
    }

    public String getReal_hours() {
        return real_hours;
    }

    public void setReal_hours(String real_hours) {
        this.real_hours = real_hours;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public String getTapd_id() {
        return tapd_id;
    }

    public void setTapd_id(String tapd_id) {
        this.tapd_id = tapd_id;
    }

    public String getTapd_type() {
        return tapd_type;
    }

    public void setTapd_type(String tapd_type) {
        this.tapd_type = tapd_type;
    }

    public String getTtms_id() {
        return ttms_id;
    }

    public void setTtms_id(String ttms_id) {
        this.ttms_id = ttms_id;
    }

    public String getCase_total() {
        return case_total;
    }

    public void setCase_total(String case_total) {
        this.case_total = case_total;
    }

    public String getPass_case_total() {
        return pass_case_total;
    }

    public void setPass_case_total(String pass_case_total) {
        this.pass_case_total = pass_case_total;
    }

    public String getNopass_case_total() {
        return nopass_case_total;
    }

    public void setNopass_case_total(String nopass_case_total) {
        this.nopass_case_total = nopass_case_total;
    }

    public String getUnable_case_total() {
        return unable_case_total;
    }

    public void setUnable_case_total(String unable_case_total) {
        this.unable_case_total = unable_case_total;
    }

    public String getBug_total() {
        return bug_total;
    }

    public void setBug_total(String bug_total) {
        this.bug_total = bug_total;
    }

    public String getBug_serious_total() {
        return bug_serious_total;
    }

    public void setBug_serious_total(String bug_serious_total) {
        this.bug_serious_total = bug_serious_total;
    }

    public String getCompare_file_path() {
        return compare_file_path;
    }

    public void setCompare_file_path(String compare_file_path) {
        this.compare_file_path = compare_file_path;
    }

    public String getDesign_id() {
        return design_id;
    }

    public void setDesign_id(String design_id) {
        this.design_id = design_id;
    }

    public String getAll_parent_id() {
        return all_parent_id;
    }

    public void setAll_parent_id(String all_parent_id) {
        this.all_parent_id = all_parent_id;
    }

    public String getAttachment_total() {
        return attachment_total;
    }

    public void setAttachment_total(String attachment_total) {
        this.attachment_total = attachment_total;
    }

    public String getTask_user() {
        return task_user;
    }

    public void setTask_user(String task_user) {
        this.task_user = task_user;
    }

    public String getIs_smart_design() {
        return is_smart_design;
    }

    public void setIs_smart_design(String is_smart_design) {
        this.is_smart_design = is_smart_design;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getEnforce_case() {
        return enforce_case;
    }

    public void setEnforce_case(String enforce_case) {
        this.enforce_case = enforce_case;
    }

    public String getTesting_expenses() {
        return testing_expenses;
    }

    public void setTesting_expenses(String testing_expenses) {
        this.testing_expenses = testing_expenses;
    }
}

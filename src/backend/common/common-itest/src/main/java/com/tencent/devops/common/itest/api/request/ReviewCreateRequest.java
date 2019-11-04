/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.common.itest.api.request;

public class ReviewCreateRequest extends BaseRequest {
    private String project_id;
    private String create_user;
    private String version_commit_time;
    private String version_desc;
    private String version_name;
    private String baseline_name;
    private String version_type;
    private String test_master;
    private String relate_process_id;

    public ReviewCreateRequest(String project_id,
            String create_user,
            String version_commit_time,
            String version_desc,
            String version_name,
            String baseline_name,
            String version_type,
            String test_master,
            String relate_process_id) {
        this.project_id = project_id;
        this.create_user = create_user;
        this.version_commit_time = version_commit_time;
        this.version_desc = version_desc;
        this.version_name = version_name;
        this.baseline_name = baseline_name;
        this.version_type = version_type;
        this.test_master = test_master;
        this.relate_process_id = relate_process_id;
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

    public String getVersion_commit_time() {
        return version_commit_time;
    }

    public void setVersion_commit_time(String version_commit_time) {
        this.version_commit_time = version_commit_time;
    }

    public String getVersion_desc() {
        return version_desc;
    }

    public void setVersion_desc(String version_desc) {
        this.version_desc = version_desc;
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

    public String getRelate_process_id() {
        return relate_process_id;
    }

    public void setRelate_process_id(String relate_process_id) {
        this.relate_process_id = relate_process_id;
    }
}

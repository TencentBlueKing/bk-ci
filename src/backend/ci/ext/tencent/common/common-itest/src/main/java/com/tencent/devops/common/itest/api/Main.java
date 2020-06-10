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

package com.tencent.devops.common.itest.api;

import com.tencent.devops.common.itest.api.pojo.ITestException;
import com.tencent.devops.common.itest.api.request.TaskCreateRequest;
import com.tencent.devops.common.itest.api.response.TaskCreateResponse;
import net.sf.cglib.beans.BeanMap;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, ITestException {
//        ITestClient client = new ITestClient("johuang", "46f1c6769101b186e3e44a0346a002b0");
//        ReviewCreateRequest request = new ReviewCreateRequest("2000", "johuanag",
//                "1526400000", "version_desc", "versionname", "baseline3",
//                "0", "jinnining", "");
//        ReviewCreateResponse response = client.createReview(request);
//
//        BeanMap beanMap = BeanMap.create(response);
//        for (Object key : beanMap.keySet()) {
//            System.out.println(key.toString() + "-->" + beanMap.get(key).toString());
//        }

        ITestClient client = new ITestClient("johuang", "46f1c6769101b186e3e44a0346a002b0");
        TaskCreateRequest request = new TaskCreateRequest("hello123", "2780",
                "ffadfafdfd", "12", "johuang", "",
                "johuang", "", "");
        TaskCreateResponse response = client.createTask(request);

        BeanMap beanMap = BeanMap.create(response);
        for (Object key : beanMap.keySet()) {
            System.out.println(key.toString() + "-->" + beanMap.get(key).toString());
        }

//        ITestClient client = new ITestClient("johuang", "46f1c6769101b186e3e44a0346a002b0");
//        ProcessCreateRequest request = new ProcessCreateRequest("2000", "johuang",
//                "versionname", "baseline2", "fdafdafdfdsfdf", "new",
//                "function test", "jinnining", "", "", "", "");
//        ProcessCreateResponse response = client.createProcess(request);
//
//        BeanMap beanMap = BeanMap.create(response);
//        for (Object key : beanMap.keySet()) {
//            System.out.println(key.toString() + "-->" + beanMap.get(key).toString());
//        }

//        ITestClient client = new ITestClient("johuang", "46f1c6769101b186e3e44a0346a002b0");
//        VersionGetRequest request = new VersionGetRequest("2000", "0");
//        VersionGetResponse response = client.getVersion(request);
//
//        BeanMap beanMap = BeanMap.create(response);
//        for (Object key : beanMap.keySet()) {
//            System.out.println(key.toString() + "-->" + beanMap.get(key).toString());
//        }

//        ITestClient client = new ITestClient("johuang", "46f1c6769101b186e3e44a0346a002b0");
//        VersionGetVersionOnlyRequest request = new VersionGetVersionOnlyRequest("2000", "0");
//        VersionGetVersionOnlyResponse response = client.getVersionOnly(request);
//
//        BeanMap beanMap = BeanMap.create(response);
//        for (Object key : beanMap.keySet()) {
//            System.out.println(key.toString() + "-->" + beanMap.get(key).toString());
//        }

//        ITestClient client = new ITestClient("johuang", "46f1c6769101b186e3e44a0346a002b0");
//        VersionGetBaselineByVersionRequest request = new VersionGetBaselineByVersionRequest("2780", "1020357512001845357",
//                "0", "0");
//        VersionGetBaselineByVersionResponse response = client.getVersionBaselineByVersion(request);
//
//        BeanMap beanMap = BeanMap.create(response);
//        for (Object key : beanMap.keySet()) {
//            System.out.println(key.toString() + "-->" + beanMap.get(key).toString());
//        }

//       ITestClient client = new ITestClient("johuang", "46f1c6769101b186e3e44a0346a002b0");
//        ProcessTestMasterRequest request = new ProcessTestMasterRequest("2780");
//        ProcessTestMasterResponse response = client.getProcessTestMaster(request);
//
//        BeanMap beanMap = BeanMap.create(response);
//        for (Object key : beanMap.keySet()) {
//            System.out.println(key.toString() + "-->" + beanMap.get(key).toString());
//        }

    }
}

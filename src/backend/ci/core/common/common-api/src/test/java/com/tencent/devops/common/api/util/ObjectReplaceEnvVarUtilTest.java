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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("all")
public class ObjectReplaceEnvVarUtilTest {

    @Test
    @SuppressWarnings("all")
    public void replaceEnvVar() {
        Map<String, String> envMap = new HashMap<>();
        envMap.put("normalStrEnvVar", "123");
        envMap.put("specStrEnvVar", "D:\\tmp\\hha");
        envMap.put("jsonStrEnvVar", "{\"abc\":\"123\"}");
        // 对普通字符串进行普通字符串变量替换
        Object originDataObj = "变量替换测试_${normalStrEnvVar}";
        Object convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(originDataObj, envMap);
        assertEquals("变量替换测试_123", JsonUtil.INSTANCE.toJson(convertDataObj));
        // 对普通字符串进行带特殊字符字符串变量替换
        originDataObj = "变量替换测试_${specStrEnvVar}";
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(originDataObj, envMap);
        assertEquals("变量替换测试_D:\\tmp\\hha", JsonUtil.INSTANCE.toJson(convertDataObj));
        // 对普通字符串进行json字符串变量替换
        originDataObj = "变量替换测试_${jsonStrEnvVar}";
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(originDataObj, envMap);
        assertEquals("变量替换测试_{\"abc\":\"123\"}", JsonUtil.INSTANCE.toJson(convertDataObj));
        // number类型变量替换
        originDataObj = "[1,2,3]";
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(originDataObj, envMap);
        System.out.println(JsonUtil.INSTANCE.toJson(convertDataObj));
        assertEquals("[ 1, 2, 3 ]", JsonUtil.INSTANCE.toJson(convertDataObj));
        // 对map对象进行变量替换
        Map<String, Object> originDataMapObj = new HashMap<>();
        originDataMapObj.put("normalStrEnvVarKey", "变量替换测试_${normalStrEnvVar}");
        originDataMapObj.put("specStrEnvVarKey", "变量替换测试_${specStrEnvVar}");
        originDataMapObj.put("jsonStrEnvVarKey1", "变量替换测试_${jsonStrEnvVar}");
        originDataMapObj.put("jsonStrEnvVarKey2", "{\"abc\":\"变量替换测试_${jsonStrEnvVar}\"}");
        originDataMapObj.put("jsonStrEnvVarKey3", "[\"变量替换测试_${jsonStrEnvVar}\"]");
        Map<String, Object> originSubDataMapObj = new HashMap<>();
        originSubDataMapObj.put("normalStrEnvVarKey", "变量替换测试_${normalStrEnvVar}");
        originSubDataMapObj.put("specStrEnvVarKey", "变量替换测试_${specStrEnvVar}");
        originSubDataMapObj.put("jsonStrEnvVarKey1", "变量替换测试_${jsonStrEnvVar}");
        originSubDataMapObj.put("jsonStrEnvVarKey2", "{\"abc\":\"变量替换测试_${jsonStrEnvVar}\"}");
        TestBean testBean = new TestBean("bean变量替换测试_${specStrEnvVar}", "{\"abc\":\"bean变量替换测试_${jsonStrEnvVar}\"}");
        originSubDataMapObj.put("testBean", testBean);
        originDataMapObj.put("originSubDataMapObj", originSubDataMapObj);
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(originDataMapObj, envMap);
        assertTrue(JsonUtil.INSTANCE.toJson(convertDataObj) instanceof String);
        // 判断map中jsonStrEnvVarKey3对应的值进行变量替换后能否正常转换为json串
        assertTrue(JsonUtil.INSTANCE.toJson(((Map<String, Object>) convertDataObj).get("jsonStrEnvVarKey3")) instanceof String);
        originSubDataMapObj = (Map<String, Object>) ((Map<String, Object>) convertDataObj).get("originSubDataMapObj");
        // 判断嵌套的map中jsonStrEnvVarKey2对应的值进行变量替换后能否正常转换为json串
        assertTrue(JsonUtil.INSTANCE.toJson(originSubDataMapObj.get("jsonStrEnvVarKey2")) instanceof String);
        // 对list对象进行变量替换
        List originDataListObj = new ArrayList();
        originDataListObj.add("变量替换测试_${normalStrEnvVar}");
        originDataListObj.add("变量替换测试_${specStrEnvVar}");
        originDataListObj.add("变量替换测试_${jsonStrEnvVar}");
        originDataListObj.add("{\"abc\":\"变量替换测试_${jsonStrEnvVar}\"}");
        originDataListObj.add("[\"变量替换测试_${jsonStrEnvVar}\"]");
        originDataListObj.add(testBean);
        Map<String, Object> dataMapObj = new HashMap<>();
        dataMapObj.put("dataMapKey", "变量替换测试_${specStrEnvVar}");
        dataMapObj.put("testBean", testBean);
        originDataListObj.add(dataMapObj);
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(originDataListObj, envMap);
        assertTrue(JsonUtil.INSTANCE.toJson(convertDataObj) instanceof String);
        assertTrue(JsonUtil.INSTANCE.toJson(((List) convertDataObj).get(3)) instanceof String);
        assertTrue(JsonUtil.INSTANCE.toJson(((List) convertDataObj).get(4)) instanceof String);
        TestBean convertTestBean = (TestBean) ((List) convertDataObj).get(5);
        assertTrue(JsonUtil.INSTANCE.toJson(convertTestBean.testBeanValue) instanceof String);
        // 对set对象进行变量替换
        Set originDataSetObj = new HashSet();
        originDataSetObj.add("变量替换测试_${normalStrEnvVar}");
        originDataSetObj.add("变量替换测试_${specStrEnvVar}");
        originDataSetObj.add("变量替换测试_${jsonStrEnvVar}");
        originDataSetObj.add("{\"abc\":\"变量替换测试_${jsonStrEnvVar}\"}");
        originDataSetObj.add("[\"变量替换测试_${jsonStrEnvVar}\"]");
        originDataSetObj.add(testBean);
        Map<String, Object> setDataMapObj = new HashMap<>();
        setDataMapObj.put("dataMapKey", "变量替换测试_${specStrEnvVar}");
        setDataMapObj.put("testBean", testBean);
        originDataSetObj.add(setDataMapObj);
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(originDataSetObj, envMap);
        assertTrue(JsonUtil.INSTANCE.toJson(convertDataObj) instanceof String);
        for (Object obj : (Set) convertDataObj) {
            if (obj instanceof TestBean) {
                TestBean testBeanObj = (TestBean) obj;
                assertTrue(JsonUtil.INSTANCE.toJson(testBeanObj.testBeanValue) instanceof String);
                assertTrue(JsonUtil.INSTANCE.toJson(testBeanObj.testBeanValue) instanceof String);
            } else {
                assertTrue(JsonUtil.INSTANCE.toJson(obj) instanceof String);
            }
        }
        // 对普通的javaBean对象进行转换
        TestComplexBean testComplexBean = new TestComplexBean();
        testComplexBean.setTestBeanKey("变量替换测试_${specStrEnvVar}");
        testComplexBean.setTestBeanKey("[\"变量替换测试_${jsonStrEnvVar}\"]");
        List dataList = new ArrayList();
        dataList.add("变量替换测试_${normalStrEnvVar}");
        dataList.add("变量替换测试_${specStrEnvVar}");
        dataList.add("变量替换测试_${jsonStrEnvVar}");
        dataList.add("{\"abc\":\"变量替换测试_${jsonStrEnvVar}\"}");
        dataList.add("[\"变量替换测试_${jsonStrEnvVar}\"]");
        testComplexBean.setDataList(dataList);
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("normalStrEnvVarKey", " 变量替换测试_${normalStrEnvVar} ");
        dataMap.put("specStrEnvVarKey", "变量替换测试_${specStrEnvVar}");
        dataMap.put("jsonStrEnvVarKey1", "变量替换测试_${jsonStrEnvVar}");
        dataMap.put("jsonStrEnvVarKey2", "{\"abc\":\"变量替换测试_${jsonStrEnvVar}\"}");
        dataMap.put("jsonStrEnvVarKey3", "[\"变量替换测试_${jsonStrEnvVar}\"]");
        Map<String, Object> subDataMap = new HashMap<>();
        subDataMap.put("normalStrEnvVarKey", "变量替换测试_${normalStrEnvVar}");
        subDataMap.put("specStrEnvVarKey", "变量替换测试_${specStrEnvVar}");
        subDataMap.put("jsonStrEnvVarKey1", "变量替换测试_${jsonStrEnvVar}");
        subDataMap.put("jsonStrEnvVarKey2", "{\"abc\":\"变量替换测试_${jsonStrEnvVar}\"}");
        testBean = new TestBean("bean变量替换测试_${specStrEnvVar}", "{\"abc\":\"bean变量替换测试_${jsonStrEnvVar}\"}");
        subDataMap.put("testBean", testBean);
        dataMap.put("subDataMap", subDataMap);
        testComplexBean.setDataMap(dataMap);
        Set dataSet = new HashSet();
        dataSet.add("变量替换测试_${normalStrEnvVar}");
        dataSet.add("变量替换测试_${specStrEnvVar}");
        dataSet.add("变量替换测试_${jsonStrEnvVar}");
        dataSet.add("{\"abc\":\"变量替换测试_${jsonStrEnvVar}\"}");
        dataSet.add("[\"变量替换测试_${jsonStrEnvVar}\"]");
        testComplexBean.setDataSet(dataSet);
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(testComplexBean, envMap);
        assertTrue(JsonUtil.INSTANCE.toJson(convertDataObj) instanceof String);
        // 魔法数字符创测试
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar("12E2", envMap);
        assertEquals("12E2", JsonUtil.INSTANCE.toJson(convertDataObj));
        // 替换”[133]-[sid-${normalStrEnvVar}]-[sid-zhiliang-test1]“带多个[]的字符串
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar("[133]-[sid-${normalStrEnvVar}]-[sid-zhiliang-test1]", envMap);
        assertEquals("[133]-[sid-123]-[sid-zhiliang-test1]", JsonUtil.INSTANCE.toJson(convertDataObj));
        // 替换包含null的对象
        dataMap = new HashMap<>();
        dataMap.put("key1", "变量");
        dataMap.put("key2", new Object[]{null, "哈哈"});
        convertDataObj = ObjectReplaceEnvVarUtil.replaceEnvVar(dataMap, envMap);
        assertTrue(JsonUtil.INSTANCE.toJson(convertDataObj) instanceof String);
    }

    static class TestBean {
        private String testBeanKey;
        private String testBeanValue;

        public TestBean() {
        }

        public TestBean(String testBeanKey, String testBeanValue) {
            this.testBeanKey = testBeanKey;
            this.testBeanValue = testBeanValue;
        }

        public String getTestBeanKey() {
            return testBeanKey;
        }

        public void setTestBeanKey(String testBeanKey) {
            this.testBeanKey = testBeanKey;
        }

        public String getTestBeanValue() {
            return testBeanValue;
        }

        public void setTestBeanValue(String testBeanValue) {
            this.testBeanValue = testBeanValue;
        }
    }

    static class TestComplexBean {
        private String testBeanKey;
        private String testBeanValue;
        private List dataList;
        private Map dataMap;
        private Set dataSet;

        public TestComplexBean() {
        }

        public TestComplexBean(String testBeanKey, String testBeanValue, List dataList, Map dataMap, Set dataSet) {
            this.testBeanKey = testBeanKey;
            this.testBeanValue = testBeanValue;
            this.dataList = dataList;
            this.dataMap = dataMap;
            this.dataSet = dataSet;
        }

        public String getTestBeanKey() {
            return testBeanKey;
        }

        public void setTestBeanKey(String testBeanKey) {
            this.testBeanKey = testBeanKey;
        }

        public String getTestBeanValue() {
            return testBeanValue;
        }

        public void setTestBeanValue(String testBeanValue) {
            this.testBeanValue = testBeanValue;
        }

        public List getDataList() {
            return dataList;
        }

        public void setDataList(List dataList) {
            this.dataList = dataList;
        }

        public Map getDataMap() {
            return dataMap;
        }

        public void setDataMap(Map dataMap) {
            this.dataMap = dataMap;
        }

        public Set getDataSet() {
            return dataSet;
        }

        public void setDataSet(Set dataSet) {
            this.dataSet = dataSet;
        }
    }
}

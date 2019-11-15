package com.tencent.bk.devops.atom.utils;

import com.tencent.bk.devops.atom.utils.json.JsonUtil;

import java.util.HashMap;
import java.util.Map;

public class Test {

    public static void main(String[] args){
        Map<String,String> dataMap = new HashMap<>();
        dataMap.put("abc","123");
        AtomTestParam atomTestParam = new AtomTestParam("hhah",dataMap);
        System.out.println(JsonUtil.toJson(atomTestParam));
        String jsonStr = "{\"bkWorkspace\":\"hhah\",\"bkSensitiveConfInfo\":{\"abc\":\"123\"}}";
        AtomTestParam atomTestParam2 = JsonUtil.fromJson(jsonStr,AtomTestParam.class);
        System.out.println(atomTestParam2.getBkSensitiveConfInfo());
    }

}

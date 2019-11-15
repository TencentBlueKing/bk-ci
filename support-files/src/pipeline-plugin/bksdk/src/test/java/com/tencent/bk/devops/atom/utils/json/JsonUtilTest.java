package com.tencent.bk.devops.atom.utils.json;

import com.google.common.collect.Lists;
import com.tencent.bk.devops.atom.utils.json.annotation.SkipLogField;
import lombok.Data;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * @version 1.0
 */
public class JsonUtilTest {

    private Bean b;

    @Before
    public void setup() {
        b = new Bean();
        b.password = "you can not see me";
        b.userName = "admin";
    }

    @Test
    public void skipLogFields() {
        String json = JsonUtil.skipLogFields(b);
        Bean bean = JsonUtil.fromJson(json, Bean.class);
        Assert.assertNull(bean.password);
    }

    @Test
    public void toJson() {
        String fullJson = JsonUtil.toJson(b);
        System.out.println(fullJson);
        Assert.assertTrue(fullJson.contains("history"));
        Bean bean = JsonUtil.fromJson(fullJson, Bean.class);
        Assert.assertEquals(b, bean);
    }

    @Test
    public void toNonEmptyJson() {
        b.history = Lists.newArrayList();//empty
        String nonEmptyValueFieldsJson = JsonUtil.toNonEmptyJson(b);
        System.out.println(nonEmptyValueFieldsJson);
        Assert.assertFalse(nonEmptyValueFieldsJson.contains("history"));
    }

    @Data
    private static class Bean {
        private String userName;
        private Integer age = 10;
        @SkipLogField
        private String password;
        private List<String> history;
    }

}

package com.tencent.bk.devops.atom.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

/**
 * @version 1.0
 */
public class AtomParamTest {

    @Test
    public void test() throws IOException {
        AtomBaseParam atomParam = new AtomBaseParam();
        atomParam.setPipelineStartUserName("pony");
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(atomParam);
        System.out.println(json);
        AtomBaseParam atomParam1 = objectMapper.readValue(json, AtomBaseParam.class);
        System.out.println(atomParam1);
    }
}

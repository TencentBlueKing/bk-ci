package com.tencent.bk.codecc.task.pojo;

import com.tencent.devops.common.constant.ComConstants.EmailNotifyTemplate;
import java.util.Map;
import java.util.Set;
import lombok.Data;

@Data
public class EmailMessageModel {
   private Set<String> receivers;
   private Set<String> cc;
   private Set<String> bcc;
   private Map<String, String> contentParam;
   private EmailNotifyTemplate template;
   private String priority;
   private Map<String, String> attaches;
}

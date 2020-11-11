package com.tencent.bk.codecc.task.model;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

@Data
@Document(collection = "t_wechat_notify_message_template")
public class WeChatNotifyMessageTemplateEntity {
   @Field("template_id")
   @Indexed
   private String templateId;

   @Field("body")
   private String body;

   @Field("sender")
   private String sender;

   @Field("source")
   private int source;

   @Field("priority")
   private String priority;

   @Field("receivers")
   private Set<String> receivers;
}

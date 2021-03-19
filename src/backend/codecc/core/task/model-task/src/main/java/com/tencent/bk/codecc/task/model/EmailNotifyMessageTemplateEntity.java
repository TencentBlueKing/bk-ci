package com.tencent.bk.codecc.task.model;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "t_email_notify_message_template")
public class EmailNotifyMessageTemplateEntity {
   @Field("template_id")
   @Indexed
   private String templateId;

   @Field("title")
   private String title;

   @Field("body")
   private String body;

   @Field("sender")
   private String sender;

   @Field("body_format")
   private String bodyFormat;
}

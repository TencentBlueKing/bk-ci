package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.EmailNotifyMessageTemplateEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailNotifyMessageTemplateRepository  extends
      MongoRepository<EmailNotifyMessageTemplateEntity, String> {
   /**
    * 根据ID获取邮件模版
    *
    * @param templateId
    */
   EmailNotifyMessageTemplateEntity findFirstByTemplateId(String templateId);
}

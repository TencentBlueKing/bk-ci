package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.WeChatNotifyMessageTemplateEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeChatNotifyMessageTemplateRepository  extends
      MongoRepository<WeChatNotifyMessageTemplateEntity, String> {
   /**
    * 根据ID获取邮件模版
    *
    * @param templateId
    */
   WeChatNotifyMessageTemplateEntity findFirstByTemplateId(String templateId);
}

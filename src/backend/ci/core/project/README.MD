# 开发者特别说明

## 因历史原因导致的混乱说明

项目因为历史原因，导致一些字段一直没有进行规范：

对于一些API接口上对项目的唯一key的定义可能存在3种：
projectId
englishName
projectCode

其实都是他们都是代表的数据库表T_PROJECT中的ENGLISH_NAME

因为projectId 目前被所有微服务api上定义为englishName，所以未来规划会将T_PROJECT表中的 PROJECT_ID 保持与 ENGLISH_NAME 一致。




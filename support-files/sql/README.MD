# 数据库脚本说明
## 脚本编写要求：

- 所有变更都要同步到ddl脚本中
- 要求：所有脚本必须是可反复执行无差别（幂等）：
  - 插入数据要求用INSERT IGNORE INTO 防止覆盖用户数据。
  - 对于需要强制刷新的系统数据可用更新语句 INSERT INTO xxx ON DUPLICATE KEY UPDATE
  - 不允许直接删除表或数据库后再重新创建的语句，只能判断后再创建：CREATE TABLE IF NOT EXISTS `表名`
  - 索引字段ALTER修改可用更新脚本存储过程，在update脚本中创建存储过程中在修改字段时先判断是否存在
- 不允许在程序不更新情况下无法运行：
  - 增加/修改字段，不允许将之设置为非空并且没有默认值
  - 不允许改名字段。
  - 删除表或字段操作，只需要更新ddl进行删除即可，不要提交update版本的脚本，防止用户误操作。


## 分类
- 创建数据库脚本：0001_{系统}\_create\-database\_{db类型}.sql
  - 示例：0001_ci_create-database_mysql.sql
- 创建表/视图脚本(ddl)：{执行序号}\_{系统}\_{模块}\_ddl\_{db类型}.sql
  - 示例：0002_ci_process_ddl_mysql.sql
- 初始化数据脚本(dml)：{执行序号}\_{系统}\_{模块}\_dml\_{db类型}.sql
  - 示例：0003_ci_process_dml_mysql.sql
- 系统发版本的更新脚本(update）：{执行序号}\_{系统}\_{模块}\_update\_{版本号}\_{db类型}.sql
  - 示例：0004_ci_process_update_v2.1.1_mysql.sql

**参数说明：**
**执行序号**： 4位数： 比如0001 按顺序下来，为方便指明执行顺序
**系统**： ci 表示是ci系统， codecc表示是codecc系统
**模块**:  process  project 等微服务模块
**版本号**： 0.1/0.2 等产品的版本号
**db类型**： mysql
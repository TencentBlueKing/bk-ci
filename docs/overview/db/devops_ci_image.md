# 数据库设计文档

**数据库名：** devops_ci_image

**文档版本：** 1.0.1

**文档描述：** devops_ci_image的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_UPLOAD_IMAGE_TASK](#T_UPLOAD_IMAGE_TASK) |  |

**表名：** <a id="T_UPLOAD_IMAGE_TASK">T_UPLOAD_IMAGE_TASK</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | TASK_ID |   varchar   | 128 |   0    |    N     |  Y   |       | 任务ID  |
|  2   | PROJECT_ID |   varchar   | 128 |   0    |    N     |  N   |       | 项目ID  |
|  3   | OPERATOR |   varchar   | 128 |   0    |    N     |  N   |       | 操作员  |
|  4   | CREATED_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  5   | UPDATED_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 修改时间  |
|  6   | TASK_STATUS |   varchar   | 32 |   0    |    N     |  N   |       | 任务状态  |
|  7   | TASK_MESSAGE |   varchar   | 256 |   0    |    Y     |  N   |       | 任务消息  |
|  8   | IMAGE_DATA |   longtext   | 2147483647 |   0    |    Y     |  N   |       | 镜像列表  |

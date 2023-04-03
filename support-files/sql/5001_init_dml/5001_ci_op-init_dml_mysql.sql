USE devops_ci_op;
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS=0;

INSERT IGNORE INTO devops_ci_op.`role` VALUES (1,'超级管理员','admin',NULL,NULL,NULL);

INSERT IGNORE INTO devops_ci_op.`schema_version` VALUES (1,'1','<< Flyway Baseline >>','BASELINE','<< Flyway Baseline >>',NULL,'root','2018-08-15 08:30:29',0,1);

INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (16,'query','访问首页','/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (17,'query','查看资源列表信息','/local/resource/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (19,'delete','删除资源','/local/resource/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (20,'add','新增资源','/local/resource/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (21,'update','修改资源','/local/resource/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (22,'query','查询角色信息','/local/role/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (23,'add','增加角色','/local/role/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (24,'update','修改角色','/local/role/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (25,'delete','删除角色','/local/role/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (26,'query','查询用户角色信息','/local/userRole/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (27,'add','为用户添加角色','/local/userRole/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (28,'update','修改用户角色信息','/local/userRole/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (29,'delete','删除用户角色信息','/local/userRole/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (30,'query','查询用户独立（非角色）权限信息','/local/userPermission/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (31,'add','新增用户独立（非角色）权限信息','/local/userPermission/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (32,'update','修改用户独立（非角色）权限信息','/local/userPermission/',NULL,NULL);
INSERT IGNORE INTO devops_ci_op.`url_action` VALUES (33,'delete','删除用户独立（非角色）权限信息','/local/userPermission/',NULL,NULL);

INSERT IGNORE INTO devops_ci_op.`user` VALUES (12,NULL,NULL,NULL,NULL,NULL,NULL,'admin');

INSERT IGNORE INTO devops_ci_op.`user_role` VALUES (18,1,9,NULL,'2019-07-12 13:34:57','2019-07-12 13:34:57');
INSERT IGNORE INTO devops_ci_op.`user_role` VALUES (21,1,12,NULL,'2019-07-12 13:34:57','2019-07-12 13:34:57');
INSERT IGNORE INTO devops_ci_op.`user_role` VALUES (24,1,14,NULL,'2019-07-12 13:34:57','2019-07-12 13:34:57');
INSERT IGNORE INTO devops_ci_op.`user_role` VALUES (25,1,15,NULL,'2019-07-12 13:34:57','2019-07-12 13:34:57');
INSERT IGNORE INTO devops_ci_op.`user_role` VALUES (26,1,16,NULL,'2019-07-12 13:34:57','2019-07-12 13:34:57');
INSERT IGNORE INTO devops_ci_op.`user_role` VALUES (27,1,17,NULL,'2019-07-12 13:34:57','2019-07-12 13:34:57');
INSERT IGNORE INTO devops_ci_op.`user_role` VALUES (28,1,18,NULL,'2019-07-12 13:34:57','2019-07-12 13:34:57');
INSERT IGNORE INTO devops_ci_op.`user_role` VALUES (29,1,19,NULL,'2019-07-15 17:53:25','2019-07-15 17:53:25');

SET FOREIGN_KEY_CHECKS=1;

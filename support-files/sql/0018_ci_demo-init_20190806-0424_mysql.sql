-- create demo project
insert into devops_ci_project.t_project
(id, created_at, project_id,project_name,english_name,project_type,approval_status)
values(1, sysdate(), '84a83354d18143f1be9f86b64c967520', 'Demo', 'demo', 5, 2);

-- insert agent
insert into devops_ci_environment.T_ENVIRONMENT_THIRDPARTY_AGENT
(ID,PROJECT_ID,NODE_ID, OS,STATUS,SECRET_KEY,CREATED_USER,CREATED_TIME,GATEWAY)
values (1, 'demo', 1, 'LINUX', 2, 'lWMvndEraCPXWsXnWRp6xUDlH5IhWfxWuGJRs/IcMIM', 'admin', sysdate(),'http://devops.bk.cloud.tencent.com');

-- insert node
insert into devops_ci_environment.T_NODE
(NODE_ID, NODE_STRING_ID, PROJECT_ID,NODE_IP,NODE_NAME, NODE_STATUS, NODE_TYPE, CREATED_USER, CREATED_TIME,OS_NAME,DISPLAY_NAME, LAST_MODIFY_TIME,LAST_MODIFY_USER)
values (1, 'demo_build_machine', 'demo', '127.0.0.1', 'demo','NORMAL','THIRDPARTY','admin',sysdate(), 'linux','demo_build_machine',sysdate(), 'admin');

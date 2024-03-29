USE devops_ci_project;
SET NAMES utf8mb4;


# bug: 社区版流水线执行失败后点重试状态不刷新 #10076 补偿脚本，如以后 web_socket 还有变化，请将该文件直接删除
UPDATE devops_ci_project.T_SERVICE t
SET t.web_socket = '^\\/console\\/pipeline\\/[^\\/]+\\/list(\\/)?(allPipeline|myPipeline|collect)?$,^\\/console\\/pipeline\\/[^\\/]+\\/[^\\/]+\\/detail\\/[^\\/]+(\\/executeDetail(\\/\\d*)?)?$,^\\/console\\/pipeline\\/[^\\/]+\\/[^\\/]+\\/history$'
WHERE t.english_name = 'Pipeline' AND t.web_socket <> '^\\/console\\/pipeline\\/[^\\/]+\\/list(\\/)?(allPipeline|myPipeline|collect)?$,^\\/console\\/pipeline\\/[^\\/]+\\/[^\\/]+\\/detail\\/[^\\/]+(\\/executeDetail(\\/\\d*)?)?$,^\\/console\\/pipeline\\/[^\\/]+\\/[^\\/]+\\/history$';


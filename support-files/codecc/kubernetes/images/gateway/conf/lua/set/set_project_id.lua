local devops_project = ngx.var.project_id

--- 获取header中的devops_projectid
if (devops_project == nil or devops_project == "") then
    devops_project = ngx.var.http_x_devops_project_id
end

--- 获取query中的devops_projectid
if (devops_project == nil or devops_project == "") then
    devops_project = ngx.var["arg_x-devops-project-id"]
end

--- 获取cookies中的devops_projectid
if (devops_project == nil or devops_project == "") then
    devops_project = cookieUtil:get_cookie("X-DEVOPS-PROJECT-ID")
end

return devops_project

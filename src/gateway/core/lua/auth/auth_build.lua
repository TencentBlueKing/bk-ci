local build_type = ngx.var.http_x_devops_build_type

ngx.header["X-DEVOPS-ERROR-RETURN"] =
    '{"status": 500,"data": "buildEnd","result":true,"message": "构建已结束。","errorCode":2101182}'
if build_type == "AGENT" then
    buildUtil:auth_agent()
    return
elseif build_type == "DOCKER" then
    buildUtil:auth_docker()
    return
elseif build_type == "PLUGIN_AGENT" then
    buildUtil:auth_plugin_agent()
    return
elseif build_type == "MACOS" then
    buildUtil:auth_macos(false)
    return
else
    buildUtil:auth_other()
    return
end


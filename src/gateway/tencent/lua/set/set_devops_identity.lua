local is_devx = string.find(ngx.var.http_host, "devx") ~= nil or ngx.var.http_x_devops_env == "devx"

if config.double_check and not is_devx and cookieUtil:get_cookie("x-devops-identity") == nil and ngx.var.http_x_tai_identity == nil then
    return "0"
else
    return "1"
end

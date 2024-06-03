-- 如果没开启二重校验 , 则不用TOF登录
if not config.double_check then
    return "1"
end

-- 如果是devx , 则不用TOF登录
local is_devx = string.find(ngx.var.http_host, "devx") ~= nil or ngx.var.http_x_devops_env == "devx"
if is_devx then
    return "1"
end

-- 如果是企微免密登录 , 则不用TOF校验
local is_wework_login = ngx.var.http_timestamp ~= nil and ngx.var.http_signature ~= nil and ngx.var.http_staffid ~= nil and
    ngx.var.http_staffname ~= nil and ngx.var.http_x_ext_data ~= nil and ngx.var.http_x_rio_seq ~= nil
if is_wework_login then
    return "1"
end

if cookieUtil:get_cookie("x-devops-identity") == nil and ngx.var.http_x_tai_identity == nil then
    return "0"
else
    return "1"
end

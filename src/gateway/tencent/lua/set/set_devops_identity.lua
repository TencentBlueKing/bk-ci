if cookieUtil:get_cookie("x-devops-identity") == nil and ngx.var.http_x_tai_identity == nil then
    return "0"
else
    return "1"
end

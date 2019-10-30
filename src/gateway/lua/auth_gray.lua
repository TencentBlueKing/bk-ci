local gray = grayUtil:get_gray()
if gray then
    ngx.header["X-DEVOPS-GRAY"] = "true"
    ngx.header["X-DEVOPS-GRAY-DIR"] = "gray"
else
    ngx.header["X-DEVOPS-GRAY"] = "false"
    ngx.header["X-DEVOPS-GRAY-DIR"] = "prod"
end
ngx.exit(200)
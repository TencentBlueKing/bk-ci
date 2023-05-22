-- 频率限制
if not accessControlUtil:isAccess() then
    ngx.log(ngx.ERR, "request excess!")
    ngx.exit(429)
    return
end

-- 安全限制
if not securityUtil:isSafe() then
    ngx.log(ngx.ERR, "unsafe request")
    ngx.exit(422)
end

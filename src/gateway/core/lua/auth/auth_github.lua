-- Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
-- Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
-- BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
-- A copy of the MIT License is included in this file.
-- Terms of the MIT License:
-- ---------------------------------------------------
-- Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
-- The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
--- Github登录对接
--- 获取Cookie中bk_token
local ci_login_token, err = cookieUtil:get_cookie("X-DEVOPS-CI-LOGIN-TOKEN")
if not ci_login_token then
    ngx.log(ngx.STDERR, "failed to read user request ci_login_token: ", err)
    ngx.exit(401)
    return
end

--- 校验信息
local token_head = "github:"
if string.sub(ci_login_token, 1, string.len(token_head)) ~= token_head then
    ngx.log(ngx.STDERR, "illegal token head , token is : ", ci_login_token)
    ngx.exit(401)
    return
end

local user_cache = ngx.shared.user_info_store
local user_cache_value = user_cache:get(ci_login_token)
if user_cache_value == nil then
    local red = redisUtil:new()
    if not red then
        ngx.log(ngx.ERR, "failed to new redis", err)
        ngx.exit(500)
        return
    end
    local redis_value = red:get("bk:login:third:key:" .. ci_login_token)
    red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
    if not redis_value or redis_value == ngx.null then
        ngx.log(ngx.ERR, "redis result is null")
        ngx.exit(401)
        return
    end
    user_cache:set(ci_login_token, redis_value, 60)
    user_cache_value = redis_value
end

--- 设置用户信息
ngx.header["x-devops-uid"] = user_cache_value
ngx.header["x-devops-bk-token"] = ci_login_token
ngx.exit(200)

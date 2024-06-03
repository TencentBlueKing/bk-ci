-- Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
-- Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
-- BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
-- A copy of the MIT License is included in this file.
-- Terms of the MIT License:
-- ---------------------------------------------------
-- Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
-- documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
-- rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
-- permit persons to whom the Software is furnished to do so, subject to the following conditions:
-- The above copyright notice and this permission notice shall be included in all copies or substantial portions of
-- the Software.
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
-- LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
-- NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
-- WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
-- SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
local redis, err = require("resty.redis")
_M = {}

local redisConfig = config.redis

function _M:new()
    if not redis then
        ngx.log(ngx.ERR, "redis require error:", err)
        return nil
    end
    local red, err = redis:new()
    if not red then
        ngx.log(ngx.ERR, "red new error:", res, err)
        return nil
    end
    red:set_timeout(2000) -- 2 second
    local res, err = red:connect(redisConfig['host'], redisConfig['port'], {
        backlog = redisConfig['backlog'],
        ssl = redisConfig['ssl'],
        pool_size = redisConfig['pool_size']
    })
    if not res then
        ngx.log(ngx.ERR, "red connect error:", redisConfig['host'], ",", redisConfig['port'], " ", err)
        return nil
    end
    if redisConfig['pass'] ~= nil then
        res, err = red:auth(redisConfig['pass'])
        if not res then
            ngx.log(ngx.ERR, "red auth error:", err)
            return nil
        end
    end

    if redisConfig['database'] ~= nil then
        res, err = red:select(redisConfig['database'])
        if not res then
            ngx.log(ngx.ERR, "red select error:", err)
            return nil
        end
    end
    return red
end

return _M

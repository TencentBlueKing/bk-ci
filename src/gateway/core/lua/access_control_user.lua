--[[
Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.

Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.

BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.

A copy of the MIT License is included in this file.


Terms of the MIT License:
---------------------------------------------------
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
]]

local _M = {}

function _M:isAccess()
    local limit_req = require "resty.limit.req"
    -- 创建req_limit实例，每秒20请求，20个等待，超过40个的请求全部拒绝
    local lim, err = limit_req.new("user_limit_req_store", 20, 20)
    -- 创建req_limit实例失败时
    if not lim then
        ngx.log(ngx.ERR,
                "failed to instantiate a resty.limit.req object: ", err)
        return false
    end
    -- 获取4字节的IP的KEY
    local key = ngx.var.uid
    -- 获取key目前的状态:delay非空的时候，说明接受请求，err是排队信息；delay为空的时候，说明拒绝请求，err是错误信息。
    local delay, err = lim:incoming(key, true)
    if not delay then
        if err == "rejected" then
            return false
        end
        ngx.log(ngx.ERR, "failed to limit req: ", err)
        return false
    end 
    if delay >= 0.001 then
    -- 排队号
        local excess = err
    -- 等待需要delay的时间
        ngx.sleep(delay)
        return true
    end

    --- 不用等待
    return true
end

return _M


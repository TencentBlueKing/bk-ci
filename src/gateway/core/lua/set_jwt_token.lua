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

if ngx.var.http_x_devops_jwt_token == nil then
  local jwt_token_cache = ngx.shared.gray_project_store
  local jwt_token_cache_value = jwt_token_cache:get("X-DEVOPS-JWT-TOKEN")
  if jwt_token_cache_value == nil then
    local table_of_jwt = {
      "header": {"typ": "JWT", "alg": "RS512"},
      "payload": {"sub": "Gateway" .. ngx. , "exp": ngx.now() + 60 * 10}
    }
    local jwt_token = jwt.sign(config.jwtPublicKey, table_of_jwt)
    jwt_token_cache.set("X-DEVOPS-JWT-TOKEN", jwt_token, 300)
    return jwt_token
  else
    return jwt_token_cache_value
  end
else
  return ngx.var.http_x_devops_jwt_token
end
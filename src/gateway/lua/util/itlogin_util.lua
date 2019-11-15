_M = {}

function _M:get_staff_info(ckey)
    local requestBody = {
        key = ckey
    }

    --- 转换请求内容
    local requestBodyJson = json.encode(requestBody)
    if requestBodyJson == nil then
        ngx.log(ngx.ERR, "failed to encode ckey request body: ", logUtil:dump(requestBody))
        ngx.exit(500)
        return
    end

    --- 初始化HTTP连接
    local httpc = http.new()
    --- 开始连接
    httpc:set_timeout(3000)
    httpc:connect(config.itlogin.ip, config.itlogin.port)
    --- 发送请求
    local res, err = httpc:request({
        path = "/devops/dm/api/user/credentialkey.php",
        method = "POST",
        headers = {
            ["Host"] = config.itlogin.host,
            ["Accept"] = "application/json",
            ["Content-Type"] = "application/x-www-form-urlencoded",
            ["X-Protocol-Version"] = "ITLoginV1.1",
            ["X-System-Key"] = "d4699d2782a6edb09a1837cfdc2df110"
        },
        body = "key=" .. ckey
    })
    --- 判断是否出错了
    if not res then
        ngx.log(ngx.ERR, "failed to request ckey info: ", err)
        ngx.exit(500)
        return
    end

    -- ngx.log(ngx.ERR, "result: ", logUtil:dump(res))
    --- 判断返回的状态码是否是200
    if res.status ~= 200 then
        ngx.log(ngx.ERR, "failed to request ckey info, status: ", res.status)
        ngx.exit(500)
        return
    end
    --- 获取所有回复
    local responseBody = res:read_body()
    --- 设置HTTP保持连接
    httpc:set_keepalive(60000, 5)
    --- 转换JSON的返回数据为TABLE
    local result = json.decode(responseBody)
    --- 判断JSON转换是否成功
    if result == nil then 
        ngx.log(ngx.ERR, "failed to parse ckey info response：", responseBody)
        ngx.exit(500)
        return
    end

    -- ngx.log(ngx.ERR, "ckey info result: ", logUtil:dump(result))
    --- 判断返回码:Q!
    if result.ReturnFlag ~= 0 then
        ngx.log(ngx.ERR, "invalid ckey info ", result.message)
        ngx.exit(401)
        return
    end
    return result
end

return _M
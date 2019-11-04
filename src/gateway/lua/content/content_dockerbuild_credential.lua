--- 初始化HTTP连接
local httpc = http.new()
local jfrogConfig = config.jfrog
--- 开始连接
httpc:set_timeout(3000)
httpc:connect(jfrogConfig.host, jfrogConfig.port)
--- 发送请求
local res, err = httpc:request({
  path = "/api/plugins/execute/createDockerUser?params=projectCode=" .. ngx.var.projectId .. ";permanent=false",
  method = "GET",
  headers = {
    ["Host"] = jfrogConfig.host,
    ["Authorization"] = jfrogConfig.auth_code,
  }
})
httpc:set_keepalive(60000, 5)
--- 判断是否出错了
if not res then
  ngx.log(ngx.ERR, "failed to request dockerbuild/credential: ", err)
  ngx.exit(500)
  return
end

--- 判断返回的状态码是否是200
if res.status ~= 200 then
  ngx.log(ngx.ERR, "Log dockerbuild/credential response http code: ", res.status)
  ngx.log(ngx.ERR, "Log dockerbuild/credential response http body: ", res:read_body())
  ngx.exit(500)
  return
end

--- 读取返回结果
local responseBody = res:read_body()
--- 转换JSON的返回数据为TABLE
local result = json.decode(responseBody)
--- 判断JSON转换是否成功
if result == nil then 
  ngx.log(ngx.ERR, "failed to parse core response：", responseBody)
  ngx.exit(500)
  return
end
--- 判断返回码
if result.status ~= 0 then
  ngx.log(ngx.ERR, "invalid dockerbuild/credential: ", result.message)
  ngx.exit(500)
  return
end
local return_result = {
  user = result.data.user,
  password = result.data.password,
  host = jfrogConfig.host,
  port = jfrogConfig.docker_port
}
ngx.say(json.encode(return_result))
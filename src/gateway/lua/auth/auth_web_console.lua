--- 获取Cookie中bk_ticket
local bk_ticket, err = cookieUtil:get_cookie("bk_ticket")
if not bk_ticket then
  ngx.log(ngx.ERR, "failed to read user request bk_ticket: ", err)
  ngx.exit(401)
  return
end

local ticket = oauthUtil:get_ticket(bk_ticket)

local resource_code = ngx.var.arg_pipelineId
if (resource_code == "" or resource_code == nil) then
  ngx.log(ngx.ERR, "Auth docker console resource_code not found: ")
  ngx.exit(403)
  return
end
local project_code = ngx.var.arg_projectId
if (project_code == "" or project_code == nil) then
  ngx.log(ngx.ERR, "Auth docker console project_code not found: ")
  ngx.exit(403)
  return
end


local verfiy = oauthUtil:verfiy_permis(project_code, "pipeline", "edit", resource_code, "pipeline", ticket.user_id, ticket.access_token)

if not verfiy then
  ngx.exit(403)
  return
end

return
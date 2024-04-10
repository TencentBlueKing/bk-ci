local in_http_or_oa = ngx.var.http_x_forwarded_proto == 'http' or string.find(ngx.var.http_host, "devops.oa.com") ~= nil

if config.openHttps == 'true' and ngx.var.agent_type == 'brower' and in_http_or_oa then
    return '1'
else
    return '0'
end

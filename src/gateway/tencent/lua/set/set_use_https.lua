local in_http = ngx.var.http_x_forwarded_proto == 'http' or ngx.var.http_protocol ~= 'https:'

if config.openHttps == 'true' and ngx.var.agent_type == 'brower' and in_http then
    return '1'
else
    return '0'
end

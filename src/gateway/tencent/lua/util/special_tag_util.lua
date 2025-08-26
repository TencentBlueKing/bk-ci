_M = {}
-- 获取特殊tag
function _M:get_special_tag(gateway_project, devops_project_id, x_gateway_tag)
    local auto_cluster_prefixes = config.kubernetes.auto_prefix
    if x_gateway_tag == nil and gateway_project ~= 'codecc' then
        -- 将前缀字符串分割成表
        local prefixes = {}
        for prefix in string.gmatch(auto_cluster_prefixes, "([^;]+)") do
            table.insert(prefixes, prefix)
        end
        -- 检查项目ID是否匹配任一前缀
        for _, prefix in ipairs(prefixes) do
            if string.find(devops_project_id, "^" .. prefix) then
                x_gateway_tag = "kubernetes-auto"
                break
            end
        end
    end
    return x_gateway_tag
end
return _M

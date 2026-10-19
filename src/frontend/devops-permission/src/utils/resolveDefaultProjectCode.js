

/**
 * 解析权限中心默认选中的项目
 * 优先级：URL 指定项目 > 用户当前访问项目（Cookie X-DEVOPS-PROJECT-ID）> 用户上次选择（localStorage 缓存）> 兜底值
 * @param {Object} options
 * @param {string} [options.routeProjectCode] - 路由（params/query）中显式指定的项目
 * @param {string} [options.cookieProjectCode] - Cookie X-DEVOPS-PROJECT-ID，用户当前访问的项目
 * @param {string} [options.cachedProjectCode] - localStorage 缓存的项目，用户上次选择
 * @param {string[]} [options.projectCodes] - 用户可访问的项目列表；提供时对 cookie/缓存候选做成员校验（URL 指定值不校验）
 * @param {string} [options.fallbackProjectCode] - 全部候选无效时的兜底值（如项目列表第一项）
 * @returns {string} 默认选中的项目 code
 */
export function resolveDefaultProjectCode({
    routeProjectCode = '',
    cookieProjectCode = '',
    cachedProjectCode = '',
    projectCodes,
    fallbackProjectCode = '',
} = {}) {
    if (routeProjectCode) return routeProjectCode;

    const candidates = [cookieProjectCode, cachedProjectCode];
    for (const code of candidates) {
        if (code && (!projectCodes || projectCodes.includes(code))) {
            return code;
        }
    }
    return fallbackProjectCode;
}

export default resolveDefaultProjectCode;

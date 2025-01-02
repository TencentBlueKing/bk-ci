const CACHE_PROJECT_CODE = 'CACHE_PROJECT_CODE';

/**
 * @param projectCode - 项目Id
 */
export function setCacheProjectCode(projectCode: string): void {
    try {
        localStorage.setItem(CACHE_PROJECT_CODE, projectCode)
    } catch (error) {
        console.error(error)
    }
}

/**
 * @returns 项目代码，如果不存在则返回 null
 */
export function getCacheProjectCode(): string | null {
    try {
        return localStorage.getItem(CACHE_PROJECT_CODE);
    } catch (error) {
        console.error(error)
        return null
    }
}
export function removeCacheProjectCode(): void {
    try {
        localStorage.removeItem(CACHE_PROJECT_CODE)
    } catch (error) {
        console.error(error)
    }
}

export const cacheProjectCode = {
    set: setCacheProjectCode,
    get: getCacheProjectCode,
    remove: removeCacheProjectCode,
};
/**
 * UI Library Compatibility Layer
 * 根据 Vue 版本动态引入对应的 UI 库
 * Vue 2.7 -> bk-magic-vue
 * Vue 3 -> bkui-vue
 */

import { version } from 'vue'

// 检测 Vue 版本
const isVue3 = version.startsWith('3.')
const isVue27 = version.startsWith('2.7')

let bkCheckbox = null
let bkPopover = null
let bkTooltips = null

// 根据 Vue 版本加载对应的 UI 库
function loadUIComponents () {
    if (isVue3) {
    // Vue 3 使用 bkui-vue
        try {
            // 动态导入 bkui-vue
            let bkuiVue
            try {
                // 尝试 ES Module 导入
                bkuiVue = require('bkui-vue')
            } catch (e) {
                bkuiVue = window.bkuiVue
            }

            // 尝试多种可能的导出方式
            bkCheckbox = bkuiVue.bkCheckbox || bkuiVue.BkCheckbox
                   || (bkuiVue.default && (bkuiVue.default.bkCheckbox || bkuiVue.default.BkCheckbox))
            bkPopover = bkuiVue.bkPopover || bkuiVue.BkPopover
                  || (bkuiVue.default && (bkuiVue.default.bkPopover || bkuiVue.default.BkPopover))
            bkTooltips = bkuiVue.bkTooltips || bkuiVue.BkTooltips
                  || (bkuiVue.default && (bkuiVue.default.bkTooltips || bkuiVue.default.BkTooltips))

            if (!bkCheckbox || !bkPopover || !bkTooltips) {
                throw new Error('Components not found in bkui-vue')
            }

            console.log('[bk-pipeline] Successfully loaded bkui-vue for Vue 3')
        } catch (error) {
            console.error('[bk-pipeline] Failed to load bkui-vue:', error.message)
            console.error('[bk-pipeline] Please install bkui-vue for Vue 3 projects:')
            console.error('[bk-pipeline]   npm install bkui-vue')
            console.error('[bk-pipeline] Documentation: https://bkui-vue.bk.tencent.com/')
            throw new Error(
                'bkui-vue is required for Vue 3 projects. '
        + 'Please install it: npm install bkui-vue'
            )
        }
    } else if (isVue27) {
    // Vue 2.7 使用 bk-magic-vue
        try {
            // 动态导入 bk-magic-vue
            let bkMagicVue
            try {
                bkMagicVue = require('bk-magic-vue')
            } catch (e) {
                throw new Error('Cannot load bk-magic-vue')
            }

            // 尝试多种可能的导出方式
            bkCheckbox = bkMagicVue.bkCheckbox
                   || (bkMagicVue.default && bkMagicVue.default.bkCheckbox)
            bkPopover = bkMagicVue.bkPopover
                  || (bkMagicVue.default && bkMagicVue.default.bkPopover)
            bkTooltips = bkMagicVue.bkTooltips
                  || (bkMagicVue.default && bkMagicVue.default.bkTooltips)
            if (!bkCheckbox || !bkPopover || !bkTooltips) {
                throw new Error('Components not found in bk-magic-vue')
            }

            console.log('[bk-pipeline] Successfully loaded bk-magic-vue for Vue 2.7')
        } catch (error) {
            console.error('[bk-pipeline] Failed to load bk-magic-vue:', error.message)
            console.error('[bk-pipeline] Please install bk-magic-vue for Vue 2.7 projects:')
            console.error('[bk-pipeline]   npm install bk-magic-vue@2.5.10-beta.12')
            console.error('[bk-pipeline] Documentation: https://magicbox.bk.tencent.com/')
            throw new Error(
                'bk-magic-vue is required for Vue 2.7 projects. '
        + 'Please install it: npm install bk-magic-vue@2.5.10-beta.12'
            )
        }
    } else {
        const errorMsg = `Unsupported Vue version: ${version}. Please use Vue 2.7+ or Vue 3+`
        console.error('[bk-pipeline]', errorMsg)
        throw new Error(errorMsg)
    }
}

// 立即加载组件
try {
    loadUIComponents()
} catch (error) {
    console.error('[bk-pipeline] Fatal error loading UI components:', error)
    // 在开发环境下抛出错误，生产环境下只记录
    if (import.meta.env.DEV) {
        throw error
    }
}

// 导出组件
export { bkCheckbox, bkPopover, bkTooltips }

// 导出版本信息（用于调试）
export const uiLibraryInfo = {
    vueVersion: version,
    isVue3,
    isVue27,
    uiLibrary: isVue3 ? 'bkui-vue' : 'bk-magic-vue',
    componentsLoaded: {
        bkCheckbox: !!bkCheckbox,
        bkPopover: !!bkPopover
    }
}

// 导出辅助函数，用于在运行时检查 UI 库
export function checkUILibrary () {
    console.log('[bk-pipeline] UI Library Info:', uiLibraryInfo)
    return uiLibraryInfo
}

import bkui from 'bkui-vue'
import bkuiEn from 'bkui-vue/dist/locale/en.esm'
import bkuiZhCn from 'bkui-vue/dist/locale/zh-cn.esm'
import { createPinia } from 'pinia'
import { createApp, h } from 'vue'
import { createI18n } from 'vue-i18n'

import { bkLoading, bkTooltips, overflowTitle } from 'bkui-vue/lib/directives'
import { VueDraggable } from 'vue-draggable-plus'
import EnUS from '../../locale/flow/en-US.json'
import JaJP from '../../locale/flow/ja-JP.json'
import ZhCN from '../../locale/flow/zh-CN.json'
import router from './router'
import { getCookies } from './utils/cookie'

// 导入全局样式
import './styles/global.css'
import './styles/utils.css'
import './styles/variables.css'

// 导入指令
import { clickoutside } from 'bkui-vue/lib/directives'
import { RouterView } from 'vue-router'
import { FLOW_GROUP_TYPES } from './constants/flowGroup'
import { ROUTE_NAMES } from './constants/routes'
import { vPerm } from './directives/perm'
import { useUIStore } from './stores'

// 语言映射配置
const localeAliasMap: Record<string, string> = {
  'zh-CN': 'zh-CN',
  'zh-cn': 'zh-CN',
  'ja-JP': 'ja-JP',
  ja: 'ja-JP',
  zh_CN: 'zh-CN',
  zh_cn: 'zh-CN',
  cn: 'zh-CN',
  'en-US': 'en-US',
  'en-us': 'en-US',
  en: 'en-US',
  us: 'en-US',
  en_US: 'en-US',
  en_us: 'en-US',
}

const bkUiLocaleAliasMap: Record<string, any> = {
  'zh-CN': bkuiZhCn,
  'zh-cn': bkuiZhCn,
  'ja-JP': bkuiEn,
  ja: bkuiEn,
  zh_CN: bkuiZhCn,
  zh_cn: bkuiZhCn,
  cn: bkuiZhCn,
  'en-US': bkuiEn,
  'en-us': bkuiEn,
  en: bkuiEn,
  us: bkuiEn,
  en_US: bkuiEn,
  en_us: bkuiEn,
}

// 获取当前语言设置
const cookiesObj = getCookies()
const i18n = createI18n({
  legacy: false,
  locale: localeAliasMap[cookiesObj.blueking_language ?? 'zh-CN'] ?? 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': ZhCN,
    'en-US': EnUS,
    'ja-JP': JaJP,
  },
})

const app = createApp({
  setup() {
    const uiStore = useUIStore()
    console.log(router.currentRoute.value, 'router.currentRoute.value')
    // 初始化时从当前路由获取 projectId
    const initProjectId = router.currentRoute.value.params.projectId as string | undefined
    if (initProjectId) {
      uiStore.setCurrentProjectId(initProjectId)
    }

    // 路由变化时同步更新 store 中的 projectId
    router.afterEach((to) => {
      const projectId = to.params.projectId as string | undefined
      if (projectId && uiStore.currentProjectId !== projectId) {
        uiStore.setCurrentProjectId(projectId)
      }
    })

    // 蓝盾选择项目时切换
    window.addEventListener('change::$currentProjectId', (e: Event) => {
      const data = (e as CustomEvent).detail
      const newProjectId = data.currentProjectId
      console.log(newProjectId, 'newProjectId', uiStore.currentProjectId)
      // 检查项目 ID 是否变化
      if (newProjectId && uiStore.currentProjectId !== newProjectId) {
        // 更新 store 中的项目 ID
        uiStore.setCurrentProjectId(newProjectId)

        // 切换到流程列表页
        router.push({
          name: ROUTE_NAMES.FLOW_LIST,
          params: {
            projectId: newProjectId,
            groupId: FLOW_GROUP_TYPES.ALL_FLOWS,
          },
        })
      }
    })

    window.addEventListener('order::backHome', () => { // 蓝盾选择项目时切换
        router.push({
            name: ROUTE_NAMES.FLOW_LIST,
        })
    })

    // window.globalVue.$on('order::syncLocale', (locale: ) => {
    //     this.$setLocale(locale, false)
    // })
  },
  render: () =>
    h(
      'div',
      {
        class: 'devops-flow-app',
      },
      [h(RouterView)],
    ),
})

app.directive('bk-tooltips', bkTooltips)
app.directive('bk-loading', bkLoading)
app.directive('overflow-title', overflowTitle)
app.use(createPinia())
app.use(router)
app.use(bkui, {
  locale: bkUiLocaleAliasMap[cookiesObj.blueking_language ?? 'zh-CN'] || bkuiZhCn,
})
app.use(i18n)

// 注册全局组件
app.component('VueDraggable', VueDraggable)

// 注册指令
app.directive('clickoutside', clickoutside)
app.directive('bk-clickoutside', clickoutside)
app.directive('perm', vPerm)

app.mount('#app')

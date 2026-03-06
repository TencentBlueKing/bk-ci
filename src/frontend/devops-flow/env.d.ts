/// <reference types="vite/client" />

declare module 'bk-permission' {
  import type { App, Directive } from 'vue'

  export function AuthorityDirectiveV3(
    handleNoPermission: (permissionData: any) => void,
    ajaxPrefix?: string,
  ): { install(app: App): void }

  export function handleNoPermissionV3(
    ui: any,
    params: Record<string, any>,
    h: typeof import('vue').h,
    data?: any,
    ajaxPrefix?: string,
  ): void
}

declare module 'bkui-vue/dist/locale/zh-cn.esm' {
  const locale: any
  export default locale
}

declare module 'bkui-vue/dist/locale/en.esm' {
  const locale: any
  export default locale
}

declare module 'bkui-vue/dist/locale/ja-jp.esm' {
  const locale: any
  export default locale
}

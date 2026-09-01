/// <reference types="vite/client" />
/// <reference types="bkui-vue/global" />

declare module 'sockjs-client' {
  interface SockJSClient {
    onclose: ((event: CloseEvent) => void) | null
    close(): void
  }

  const SockJS: new (url: string) => SockJSClient
  export default SockJS
}

declare module 'stompjs/lib/stomp.js' {
  export interface StompFrame {
    body: string
  }

  export interface StompClient {
    connected: boolean
    debug: unknown
    connect(
      headers: Record<string, string>,
      onConnected: () => void,
      onError: (error: unknown) => void,
    ): void
    subscribe(destination: string, callback: (frame: StompFrame) => void): void
    send(destination: string, headers: Record<string, string>, body: string): void
    disconnect(callback?: () => void): void
  }

  export const Stomp: {
    over(socket: unknown): StompClient
  }
}

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

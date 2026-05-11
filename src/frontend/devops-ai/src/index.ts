import type { App as VueApp, Plugin } from 'vue'
import CiAiApp from './App'

export { CiAiApp }

export const CiAiPlugin: Plugin = {
  install(app: VueApp) {
    app.component('CiAi', CiAiApp)
  },
}

export default CiAiApp

import { createApp } from 'vue'
import bkui from 'bkui-vue'
import 'bkui-vue/dist/style.variable.css'
import '@blueking/chat-x/dist/index.css'
import './styles/variables.css'
import App from './App'

const app = createApp(App)
app.use(bkui)
app.mount('#app')

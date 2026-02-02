/* eslint-disable vue/multi-word-component-names */
import layoutStyles from '@/styles/layout.module.css'
import { defineComponent } from 'vue'
import { RouterView } from 'vue-router'

export default defineComponent({
  setup() {
    return () => (
      <div class={layoutStyles.page}>
          <RouterView />
      </div>
    )
  },
})

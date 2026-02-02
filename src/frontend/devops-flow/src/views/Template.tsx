import { defineComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { FlowGroupAside } from '../components/FlowGroupAside'
import styles from '@/styles/layout.module.css'

export default defineComponent({
  name: 'Template',
  setup() {
    const { t } = useI18n()

    return () => (
      <div class={styles.page}>
        <div class={styles.sidebar}>
          <FlowGroupAside />
        </div>
        <div class={styles.content}>
          <div style="padding: 24px;">
            <h2>{t('flow.tabs.template')}</h2>
          </div>
        </div>
      </div>
    )
  },
})

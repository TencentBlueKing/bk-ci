import { defineComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { Exception } from 'bkui-vue'
import styles from './EmptyPage.module.css'

export default defineComponent({
  name: 'EmptyPage',
  props: {
    title: {
      type: String,
      default: '',
    },
    desc: {
      type: String,
      default: '',
    },
  },
  setup(props, { slots }) {
    const { t } = useI18n()
    return () => (
      <Exception type="empty" class={styles.exceptionCont}>
        <div class={styles.exception}>{props.title ? props.title : t('flow.common.noData')}</div>
        {props.desc ? <p class={styles.desc}>{props.desc}</p> : null}
        {slots.default?.()}
      </Exception>
    )
  },
})

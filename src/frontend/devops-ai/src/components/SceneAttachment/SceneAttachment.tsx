import { defineComponent, ref } from 'vue'
import styles from './SceneAttachment.module.css'

export default defineComponent({
  name: 'SceneAttachment',
  props: {
    title: { type: String, default: '' },
    body: { type: String, default: '' },
  },
  setup(props) {
    const collapsed = ref(true)

    return () => {
      if (!props.title && !props.body) return null
      return (
        <div class={[styles.root, collapsed.value && styles.collapsed]}>
          <button
            type="button"
            class={styles.header}
            onClick={() => { collapsed.value = !collapsed.value }}
          >
            <span class={[styles.arrow, !collapsed.value && styles.arrowExpanded]}>▶</span>
            <span class={styles.title}>{props.title}</span>
          </button>
          {!collapsed.value && props.body ? (
            <pre class={styles.body}>{props.body}</pre>
          ) : null}
        </div>
      )
    }
  },
})

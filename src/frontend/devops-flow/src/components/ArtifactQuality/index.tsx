import { defineComponent, type PropType} from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './ArtifactQuality.module.css'

interface QualityListValue {
  labelKey: string
  value: string
  color: string
  count: number
}

interface QualityData {
  [title: string]: QualityListValue[]
}

export default defineComponent({
  name: 'ArtifactQuality',
  props: {
    data: {
      type: Object as PropType<QualityData>,
      default: () => ({}),
    },
  },
  emits: ['goOutputs'],
  setup(props, { emit }) {
    const { t } = useI18n()

    const goOutputs = (values: QualityListValue[]) => {
      emit('goOutputs', values)
    }

    return () => (
      <ul class={styles.qualityList}>
        {Object.entries(props.data).map(([title, values]) => (
          <li key={title} class={styles.qualityTags} onClick={() => goOutputs(values)}>
            <span class={styles.tagLabel}>{title}</span>
            {Array.isArray(values) &&
              values.map((tag, index) => (
                <div key={index} class={styles.colorItem}>
                  <span class={styles.colorBlock} style={{ backgroundColor: tag.color }}>
                    {tag.count}
                  </span>
                </div>
              ))}
          </li>
        ))}
      </ul>
    )
  },
})

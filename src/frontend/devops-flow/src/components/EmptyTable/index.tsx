import { defineComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { Exception } from 'bkui-vue'
import styles from './EmptyTable.module.css'

export default defineComponent({
  name: 'EmptyTableStatus',
  props: {
    type: {
      type: String as () => 'empty' | 'search-empty',
      default: 'empty',
    },
  },
  emits: ['clear'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const typeMap = {
      empty: t('flow.common.noData'),
      'search-empty': t('flow.searchResultsEmpty'),
    }
    function handleClear() {
      emit('clear')
    }

    return () => (
      <Exception type={props.type} class={styles.exceptionCont}>
        <div class={styles.exception}>{typeMap[props.type]}</div>
        {props.type === 'search-empty' ? (
          <div>
            <i18n-t
              tag="div"
              keypath="flow.tryAdjustingKeywordsOrClearingFilters"
              class={styles.tips}
            >
              <button class={styles.clearBtn} onClick={handleClear}>
                {t('flow.clearFilterCriteria')}
              </button>
            </i18n-t>
          </div>
        ) : undefined}
      </Exception>
    )
  },
})

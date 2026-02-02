import { SvgIcon } from '@/components/SvgIcon'
import useAuthoringEnvironment, { type EnvSelectItem } from '@/hooks/useAuthoringEnvironment'
import { Loading, Select, Tag } from 'bkui-vue'
import { computed, defineComponent, nextTick, onMounted, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './AuthoringEnv.module.css'

export default defineComponent({
  name: 'AuthoringEnv',
  components: {
    SvgIcon,
  },
  props: {
    isEdit: {
      type: Boolean,
      default: false,
    },
    modelValue: {
      type: String,
      default: '',
    },
    envList: {
      type: Array as PropType<EnvSelectItem[]>,
      default: () => [],
    },
    nodeList: {
      type: Array,
      default: () => [],
    },
    envLoading: {
      type: Boolean,
      default: false,
    },
    nodeLoading: {
      type: Boolean,
      default: false,
    }
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const envHashId = ref(props.modelValue)
    const { goEnvironment, loadNodeList } = useAuthoringEnvironment()
    const envName = computed(() => {
      return props.envList.find(env => env.envHashId === envHashId.value)?.name || envHashId.value
    })

    watch(
      () => props.modelValue,
      (newValue) => {
        if (newValue !== envHashId.value) {
          envHashId.value = newValue
          nextTick(() => {
            loadNodeList(newValue)
          })
        }
      },
    )

    onMounted(() => {
      if (envHashId.value) {
        loadNodeList(envHashId.value)
      }
    })

    function handleChange() {
      emit('update:modelValue', envHashId.value)
      nextTick(() => {
          loadNodeList(envHashId.value)
      })
    }

    function goToEnvironment() {
      goEnvironment(envHashId.value)
    }

    return () => (
      <div class={styles.authoringContent}>
        <p class={styles.authoringHeader}>
          {props.isEdit ? (
            <Select
              v-model={envHashId.value}
              filterable
              list={props.envList}
              loading={props.envLoading}
              searchPlaceholder={t('flow.content.searchEnvironment')}
              onChange={handleChange}
            >
            </Select>
          ) : (
            <span class={styles.headerText}>{envName.value}</span>
          )}
        </p>
        {envHashId.value ? (
          <Loading loading={props.nodeLoading} size="small" class="p-lg">
            <div class={styles.envItem}>
              <p class={styles.envItemTit}>
                {t('flow.content.creationNode')}
                {props.isEdit ? (
                  <span onClick={goToEnvironment}>
                    <SvgIcon name="set-line" size={12} class={`cursor-pointer ${styles.setLine}`} />
                  </span>
                ) : null}
              </p>
              <div class={styles.nodeTag}>
                {props.nodeList.length > 0
                  ? props.nodeList.map((node: any) => (
                      <Tag key={node.nodeId}>{node.displayName}</Tag>
                    ))
                  : '--'}
              </div>
            </div>
            <div class={styles.envItem}>
              <p class={styles.envItemTit}>{t('flow.content.workspace')}</p>
              <div>{t('flow.content.workSpaceDesc')}</div>
            </div>
          </Loading>
        ) : (
          <p class={styles.noData}>{t('flow.content.previewDetailsAfterEnvironmentSelection')}</p>
        )}
      </div>
    )
  },
})

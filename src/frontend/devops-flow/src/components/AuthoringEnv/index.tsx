import { getAuthoringNodeDisplayText } from '@/api/authoringEnvironmentApi'
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
    },
    selectLabel: {
      type: String,
      default: '',
    },
    selectRequired: {
      type: Boolean,
      default: false,
    },
    showEnvironmentManagement: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const envHashId = ref(props.modelValue)
    const { goEnvironment, loadNodeList } = useAuthoringEnvironment()
    const envName = computed(() => {
      return props.envList.find((env) => env.envHashId === envHashId.value)?.name || envHashId.value
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

    function renderEnvironmentManagement() {
      return (
        <button type="button" class={styles.envManageEntry} onClick={goToEnvironment}>
          <SvgIcon name="jump" size={12} class={styles.envManageIcon} />
          <span>{t('flow.content.environmentManagement')}</span>
        </button>
      )
    }

    function renderEnvSelect() {
      const selectProps = {
        class: styles.envSelect,
        modelValue: envHashId.value,
        'onUpdate:modelValue': (value: string) => {
          envHashId.value = value
        },
        filterable: true,
        list: props.envList,
        loading: props.envLoading,
        placeholder: t('flow.content.creationEnvironment'),
        searchPlaceholder: t('flow.content.searchEnvironment'),
        onChange: handleChange,
      }
      const select = props.showEnvironmentManagement ? (
        <Select {...selectProps}>
          {{
            extension: renderEnvironmentManagement,
          }}
        </Select>
      ) : (
        <Select
          {...selectProps}
        ></Select>
      )

      return (
        <div class={[styles.envSelectLine, !props.selectLabel && styles.envSelectLinePlain]}>
          {props.selectLabel ? (
            <label class={styles.envSelectLabel}>
              {props.selectLabel}
              {props.selectRequired ? <span class={styles.requiredMark}>*</span> : null}
            </label>
          ) : null}
          <div class={styles.envSelectControl}>{select}</div>
        </div>
      )
    }

    return () => (
      <div class={styles.authoringRoot}>
        {props.isEdit ? renderEnvSelect() : null}
        <div class={styles.authoringContent}>
          {!props.isEdit ? (
            <p class={styles.authoringHeader}>
              <span class={styles.headerText}>{envName.value}</span>
            </p>
          ) : null}
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
                        <Tag key={node.nodeId}>{getAuthoringNodeDisplayText(node)}</Tag>
                      ))
                    : '--'}
                </div>
              </div>
              <div class={styles.envItem}>
                <p class={styles.envItemTit}>{t('flow.content.workspace')}</p>
                <div>{t('flow.content.workSpaceDesc', [envHashId.value])}</div>
              </div>
            </Loading>
          ) : (
            <p class={styles.noData}>{t('flow.content.previewDetailsAfterEnvironmentSelection')}</p>
          )}
        </div>
      </div>
    )
  },
})

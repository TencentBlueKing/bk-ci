import { REPOSITORY_API_URL_PREFIX } from '@/utils/apiUrlPrefix'
import { Select } from 'bkui-vue'
import { computed, defineComponent, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './CodelibSelector.module.css'
import Selector from './Selector'
import VuexInput from './VuexInput'

type RepositoryType = 'ID' | 'NAME' | 'SELF'

export default defineComponent({
  name: 'CodelibSelector',
  props: {
    // 当前字段名（通常为 repositoryType），由 AtomForm 注入
    name: {
      type: String,
      default: 'repositoryType',
    },
    // 整个插件表单的值集合，用于读取 repositoryType / repoHashId / repoName
    atomValue: {
      type: Object as PropType<Record<string, any>>,
      default: () => ({}),
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    handleChange: {
      type: Function as PropType<(name: string, value: any) => void>,
      default: () => () => {},
    },
  },
  setup(props) {
    const { t } = useI18n()

    const repositoryType = computed<RepositoryType>(
      () => (props.atomValue.repositoryType as RepositoryType) || 'ID',
    )

    const codeRepoUrl = `${REPOSITORY_API_URL_PREFIX}/user/repositories/{projectId}/hasPermissionList?permission=USE&page=1&pageSize=1000`

    const codelibConfigList = computed(() => [
      { value: 'ID', label: t('flow.codelibSelector.selectRepo') },
      { value: 'NAME', label: t('flow.codelibSelector.enterAlias') },
      { value: 'SELF', label: t('flow.codelibSelector.monitorPac') },
    ])

    const handleChangeRepositoryType = (val: string | number) => {
      props.handleChange('branches', [])
      props.handleChange('repoHashId', '')
      props.handleChange('repoName', '')
      props.handleChange('repositoryType', val)
    }

    const handleChangeRepoHashId = (name: string, val: any) => {
      props.handleChange(name, val)
      props.handleChange('branches', [])
    }

    return () => (
      <div class={styles.codelibSelector}>
        <Select
          class={styles.groupBox}
          modelValue={repositoryType.value}
          clearable={false}
          disabled={props.disabled}
          onChange={handleChangeRepositoryType}
        >
          {codelibConfigList.value.map((item) => (
            <Select.Option key={item.value} value={item.value} label={item.label} />
          ))}
        </Select>
        {repositoryType.value === 'ID' ? (
          <Selector
            class={styles.inputSelector}
            name="repoHashId"
            value={props.atomValue.repoHashId ?? ''}
            disabled={props.disabled}
            atomValue={props.atomValue}
            optionsConf={{
              url: codeRepoUrl,
              paramId: 'repositoryHashId',
              paramName: 'aliasName',
              searchable: true,
            }}
            handleChange={handleChangeRepoHashId}
          />
        ) : (
          <VuexInput
            key={repositoryType.value}
            class={styles.inputSelector}
            name="repoName"
            value={props.atomValue.repoName ?? ''}
            disabled={repositoryType.value === 'SELF'}
            placeholder={
              repositoryType.value === 'SELF'
                ? t('flow.codelibSelector.pacRepoNotSet')
                : t('flow.codelibSelector.enterRepoAlias')
            }
            handleChange={props.handleChange}
          />
        )}
      </div>
    )
  },
})

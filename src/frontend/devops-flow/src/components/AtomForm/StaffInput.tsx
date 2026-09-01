import { get } from '@/utils/http'
import { Message, TagInput } from 'bkui-vue'
import { computed, defineComponent, onMounted, type PropType, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import styles from './StaffInput.module.css'

interface StaffInfo {
  english_name: string
  chinese_name: string
}

const USER_IMG_URL = '//rhrc.woa.com/photo/150'
const OPEN_URL = '//open.woa.com'
const OIED_URL = '//o.ied.com'

const BK_VAR_PATTERN = /\$\{\{[\w_.-]+\}\}|\$\{[\w_.-]+\}/

function isBkVar(str: string): boolean {
  return BK_VAR_PATTERN.test(str)
}

function jsonp<T>(url: string, params: Record<string, string>): Promise<T> {
  return new Promise((resolve, reject) => {
    const callbackName = `jsonp_${Date.now()}_${Math.random().toString(36).slice(2)}`
    const query = new URLSearchParams({ ...params, callback: callbackName }).toString()
    const script = document.createElement('script')
    script.src = `${url}?${query}`
    ;(window as any)[callbackName] = (data: T) => {
      resolve(data)
      document.head.removeChild(script)
      delete (window as any)[callbackName]
    }
    script.onerror = (err) => {
      reject(err)
      document.head.removeChild(script)
      delete (window as any)[callbackName]
    }
    document.head.appendChild(script)
  })
}

let staffListCache: StaffInfo[] | null = null
let staffListPromise: Promise<StaffInfo[]> | null = null

async function fetchStaffList(): Promise<StaffInfo[]> {
  if (staffListCache) return staffListCache
  if (staffListPromise) return staffListPromise

  staffListPromise = (async () => {
    try {
      const prefix = `${location.host.includes('o.ied.com') ? OIED_URL : OPEN_URL}/component/compapi/tof3`
      const result = await jsonp<{ data: StaffInfo[] }>(`${prefix}/get_all_staff_info`, {
        query_type: 'simple_data',
        app_code: 'workbench',
      })
      staffListCache = result.data
      return staffListCache
    } catch {
      staffListPromise = null
      return []
    }
  })()

  return staffListPromise
}

function getAvatarUrl(name: string): string {
  const member = isBkVar(name) ? 'un_know' : name
  return `${USER_IMG_URL}/${member}.png?default_when_absent=true`
}

export default defineComponent({
  name: 'StaffInput',
  props: {
    value: {
      type: [Array, String] as PropType<string[] | string>,
      default: () => [],
    },
    name: {
      type: String,
      required: true,
    },
    placeholder: {
      type: String,
      default: '',
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    prependText: {
      type: String,
      default: '',
    },
    handleChange: {
      type: Function,
      default: () => () => {},
    },
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const isLoading = ref(false)
    const list = ref<StaffInfo[]>([])
    const normalizedValue = computed(() => {
      if (Array.isArray(props.value)) return props.value
      if (typeof props.value === 'string' && props.value) return props.value.split(',')
      return []
    })

    const projectId = computed(() => route.params.projectId as string)

    const init = async () => {
      if (isLoading.value) return
      try {
        isLoading.value = true
        list.value = await fetchStaffList()
      } catch (error) {
        console.error(error)
      } finally {
        isLoading.value = false
      }
    }

    const detectIsInProject = async (val: string): Promise<boolean> => {
      try {
        return await get<boolean>(
          `/project/api/user/projects/${projectId.value}/users/${val}/verify`,
        )
      } catch {
        return false
      }
    }

    const detect = (val: string): boolean => {
      if (val.startsWith('$')) {
        return isBkVar(val)
      }
      return true
    }

    const applyChange = (val: string[]) => {
      emit('update:value', val)
      emit('change', val)
      props.handleChange(props.name, val)
    }

    const handleSelect = async (value: string[]) => {
      try {
        const currentValueMap = normalizedValue.value.reduce<Record<string, boolean>>((acc, item) => {
          acc[item] = true
          return acc
        }, {})

        const res = await Promise.all(
          value.map((item) => {
            if (currentValueMap[item] || isBkVar(item)) return true
            return detectIsInProject(item)
          }),
        )

        const invalidUser = value.filter((_, index) => !res[index]).join(',')
        if (invalidUser) {
          Message({
            theme: 'error',
            message: t('flow.common.unAccessUser', [invalidUser]),
          })
        }

        applyChange(value.filter((_, index) => res[index]))
      } catch (error) {
        console.error(error)
        applyChange(normalizedValue.value)
      }
    }

    const paste = (val: string): string[] => {
      const newValues = val
        .split(',')
        .map((v) => v.trim())
        .filter((v) => v && !props.value.includes(v))
      handleSelect([...props.value, ...newValues])
      return []
    }

    const renderMemberTag = (node: StaffInfo) => (
      <div class={styles.selectedStaffTag}>
        <img src={getAvatarUrl(node.english_name)} />
        <span>{node.english_name}</span>
      </div>
    )

    const renderMemberList = (node: StaffInfo) => (
      <div class={`bk-selector-node ${styles.selectorMember}`}>
        <img class={styles.avatar} src={getAvatarUrl(node.english_name)} />
        <span class={styles.text}>
          {node.english_name} ({node.chinese_name})
        </span>
      </div>
    )

    onMounted(init)

    return () => (
      <div class={styles.staffInput}>
        {props.prependText && <div class={styles.prependBox}>{props.prependText}</div>}
        <TagInput
          modelValue={normalizedValue.value}
          placeholder={props.placeholder}
          disabled={props.disabled || isLoading.value}
          allowCreate={true}
          hasDeleteIcon={true}
          saveKey="english_name"
          displayKey="chinese_name"
          searchKey="english_name"
          list={list.value}
          tagTpl={renderMemberTag}
          tpl={renderMemberList}
          createTagValidator={detect}
          pasteFn={paste}
          copyable={false}
          separator=","
          onChange={handleSelect}
        />
      </div>
    )
  },
})

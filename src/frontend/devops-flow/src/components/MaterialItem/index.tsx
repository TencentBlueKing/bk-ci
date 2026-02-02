import { SvgIcon } from '@/components/SvgIcon'
import { getMaterialIconByType } from '@/utils/util'
import { Link, Popover } from 'bkui-vue'
import { computed, defineComponent } from 'vue'
import styles from './MaterialItem.module.css'

interface MaterialInfoKey {
  aliasName?: string
  branchName?: string
  newCommitId?: string
  webhookAliasName?: string
  webhookBranch?: string
  webhookCommitId?: string
  webhookSourceBranch?: string
  mrIid?: string
  tagName?: string
  noteId?: string
  issueIid?: string
  reviewId?: string
  webhookSourceTarget?: string
  parentPipelineName?: string
  parentBuildNum?: string
  materialName?: string
  materialId?: string
  [key: string]: string | undefined
}

export default defineComponent({
  name: 'MaterialItem',
  props: {
    isWebhook: Boolean,
    isFitContent: {
      type: Boolean,
      default: true,
    },
    showMore: {
      type: Boolean,
      default: true,
    },
    showMorePlaceholder: {
      type: Boolean,
      default: false,
    },
    material: {
      type: Object,
      required: true,
    },
  },
  emits: ['mouseEnter', 'click'],
  setup(props, { emit }) {
    const scmType = computed(() =>
      props.isWebhook ? `CODE_${props.material?.codeType}` : props.material?.scmType,
    )
    const isMR = computed(() => {
      return ['MERGE_REQUEST', 'PULL_REQUEST', 'MERGE_REQUEST_ACCEPT'].includes(
        props.material?.webhookEventType,
      )
    })
    const isSVN = computed(() => scmType.value === 'CODE_SVN')
    const materialInfoKeys = computed<string[]>(() => {
      if (!props.isWebhook) {
        return ['aliasName', ...(isSVN.value ? [] : ['branchName']), 'newCommitId']
      }
      switch (props.material?.webhookEventType) {
        case 'PUSH':
          return ['webhookAliasName', 'webhookBranch', 'webhookCommitId']
        case 'MERGE_REQUEST':
        case 'PULL_REQUEST':
          return ['webhookAliasName', 'webhookSourceTarget', 'mrIid']
        case 'MERGE_REQUEST_ACCEPT':
          return ['webhookAliasName', 'webhookSourceTarget', 'mrIid']
        case 'TAG_PUSH':
          return ['webhookAliasName', 'webhookBranch', 'tagName']
        case 'NOTE':
          return ['webhookAliasName', 'noteId']
        case 'ISSUES':
          return ['webhookAliasName', 'issueIid']
        case 'REVIEW':
          return ['webhookAliasName', 'reviewId']
        case 'POST_COMMIT':
          return ['webhookAliasName', 'webhookCommitId']
        case 'CHANGE_COMMIT':
          return ['webhookAliasName', 'webhookCommitId']
        case 'PARENT_PIPELINE':
          return ['parentPipelineName', 'parentBuildNum']
        default:
          return props.material?.materialId
            ? ['materialName', 'materialId']
            : ['webhookAliasName', 'webhookBranch']
      }
    })
    const iconArray = computed<MaterialInfoKey>(() => {
      const scmIcon = getMaterialIconByType(scmType.value)
      return {
        aliasName: scmIcon,
        branchName: 'branch',
        newCommitId: 'commit',
        webhookAliasName: scmIcon,
        webhookBranch: 'branch',
        webhookCommitId: 'commit',
        webhookSourceBranch: 'branch',
        mrIid: 'webhook-mr',
        tagName: 'webhook-tag',
        noteId: 'webhook-note',
        issueIid: 'webhook-issue',
        reviewId: 'webhook-review',
        webhookSourceTarget: 'branch',
        parentPipelineName: 'pipeline',
        parentBuildNum: 'sharp',
        materialName: scmIcon,
        materialId: 'link',
      }
    })
    const materialInfoValueMap = computed<Record<string, string>>(() => {
      return materialInfoKeys.value.reduce(
        (acc, key) => {
          acc[key] = formatField(key)
          return acc
        },
        {} as Record<string, string>,
      )
    })

    const emitMouseEnter = () => {
      emit('mouseEnter')
    }

    const emitClick = () => {
      emit('click')
    }

    const getLink = (field: string) => {
      switch (field) {
        case 'newCommitId':
          return props.material?.url ?? ''
        default:
          return props.material?.linkUrl ?? ''
      }
    }

    const includeLink = (field: string) => {
      return (
        [
          'newCommitId',
          'reviewId',
          'issueIid',
          'noteId',
          'mrIid',
          'tagName',
          'webhookCommitId',
          'parentBuildNum',
        ].includes(field) &&
        !isSVN.value &&
        getLink(field)
      )
    }

    const formatField = (field: string) => {
      switch (field) {
        case 'reviewId':
        case 'issueIid':
        case 'noteId':
        case 'mrIid':
          return `[${props.material[field]}]`
        case 'newCommitId':
        case 'webhookCommitId':
          return props.material?.[field]?.slice?.(0, 8) ?? '--'
        default:
          return props.material?.[field] ?? '--'
      }
    }

    const handleToLink = (field: string) => {
      if (field === 'materialId') {
        window.open(getLink(field), '_blink')
      }
    }

    return () => (
      <div class={[styles.execMaterialRow, props.isFitContent ? styles.fitContent : '']}>
        <div class={styles.materialRowInfoSpans}>
          {materialInfoKeys.value.map((field) => (
            <span key={field}>
              <SvgIcon name={iconArray.value[field] || 'commit'} size={14} />
              {includeLink(field) ? (
                <Link
                  class={styles.materialSpan}
                  theme="primary"
                  target="_blank"
                  href={getLink(field)}
                >
                  {formatField(field)}
                </Link>
              ) : isMR.value && field === 'webhookSourceTarget' ? (
                <span class={styles.mrSourceTarget}>
                  <span
                    v-bk-tooltips={{
                      delay: [300, 0],
                      content: props.material.webhookSourceBranch,
                      allowHTML: false,
                    }}
                  >
                    {props.material.webhookSourceBranch}
                  </span>
                  <SvgIcon name="arrows-right" size={14} />
                  <SvgIcon name={iconArray.value[field] || 'commit'} size={14} />
                  <span
                    v-bk-tooltips={{
                      delay: [300, 0],
                      content: props.material.webhookBranch,
                      allowHTML: false,
                    }}
                  >
                    {props.material.webhookBranch}
                  </span>
                </span>
              ) : (
                <Popover
                  content={materialInfoValueMap.value[field]}
                  delay={[300, 0]}
                  class={styles.materialSpanTooltipBox}
                >
                  {{
                    default: () => (
                      <span
                        class={[
                          styles.materialSpan,
                          field === 'materialId' ? styles.materialUrl : '',
                        ]}
                        onClick={() => handleToLink(field)}
                      >
                        {materialInfoValueMap.value[field]}
                      </span>
                    ),
                  }}
                </Popover>
              )}
            </span>
          ))}
        </div>
        {(props.showMore || props.showMorePlaceholder) && (
          <span
            onMouseenter={props.showMore ? emitMouseEnter : undefined}
            onClick={props.showMore ? emitClick : undefined}
            class={styles.execMoreMaterial}
            style={{ visibility: props.showMore ? 'visible' : 'hidden' }}
          >
            <SvgIcon name="ellipsis" size={14} />
          </span>
        )}
      </div>
    )
  },
})

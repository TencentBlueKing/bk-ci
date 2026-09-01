/**
 * LogHeader Component
 * 日志面板头部组件 - 统一管理日志面板的头部样式和功能
 * 支持：
 * - 标题（状态图标 + 名称）
 * - 可选的标签页（日志/配置）
 * - 搜索栏
 * - 执行次数选择器
 * - 更多按钮（下拉菜单）
 * - 关闭按钮
 * - 通过 slot 支持自定义内容
 */
import { DETAIL_TAB, type DetailTabType } from '@/components/ExecDetail/constants'
import StatusIcon from '@/components/StatusIcon'
import { SvgIcon } from '@/components/SvgIcon'
import type { StatusType } from '@/types/flow'
import { Input, Select } from 'bkui-vue'
import { computed, defineComponent, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './LogHeader.module.css'

export interface SearchState {
  keyword: string
  matches: number[]
  currentIndex: number
}

export default defineComponent({
  name: 'LogHeader',
  props: {
    // 标题相关
    title: {
      type: String,
      required: true,
    },
    status: {
      type: String as PropType<StatusType>,
      default: 'QUEUE',
    },
    // 标签页相关
    showTabs: {
      type: Boolean,
      default: false,
    },
    activeTab: {
      type: String as PropType<DetailTabType>,
      default: DETAIL_TAB.LOG,
    },
    // 搜索相关
    showSearchBar: {
      type: Boolean,
      default: true,
    },
    searchKeyword: {
      type: String,
      default: '',
    },
    searchMatches: {
      type: Array as PropType<number[]>,
      default: () => [],
    },
    currentMatchIndex: {
      type: Number,
      default: -1,
    },
    // 执行次数相关
    showExecuteSelector: {
      type: Boolean,
      default: false,
    },
    executeCount: {
      type: Number,
      default: 1,
    },
    currentExecute: {
      type: Number,
      default: 1,
    },
    // 更多按钮相关
    showMoreButton: {
      type: Boolean,
      default: false,
    },
    showMoreMenu: {
      type: Boolean,
      default: false,
    },
    showDebug: {
      type: Boolean,
      default: false,
    },
    showTime: {
      type: Boolean,
      default: undefined,
    },
  },
  emits: [
    'tab-change',
    'search-change',
    'search-prev',
    'search-next',
    'execute-change',
    'toggle-debug',
    'toggle-time',
    'toggle-more-menu',
    'download',
  ],
  setup(props, { emit, slots }) {
    const { t } = useI18n()

    // 搜索结果计数
    const searchResultText = computed(() => {
      if (!props.searchKeyword || props.searchMatches.length === 0) return '0 / 0'
      return `${props.currentMatchIndex + 1}/${props.searchMatches.length}`
    })

    // Render execute count options
    const renderExecuteCountOptions = () => {
      const options = []
      for (let i = 1; i <= props.executeCount; i++) {
        options.push(
          <Select.Option key={i} id={i} name={`${i}`}>
            {i}
          </Select.Option>,
        )
      }
      return options
    }

    // Render search bar
    const renderSearchBar = () => {
      return (
        <div class={styles.logSearchBar}>
          <Input
            modelValue={props.searchKeyword}
            onUpdate:modelValue={(val: string) => emit('search-change', val)}
            placeholder="Search"
            class={styles.logSearchInput}
            clearable={false}
          />
          <span class={styles.logSearchNav}>
            <button
              class={styles.logSearchNavBtn}
              onClick={() => emit('search-prev')}
              disabled={props.searchMatches.length === 0}
            >
              ‹
            </button>
            <span class={styles.logSearchCount}>{searchResultText.value}</span>
            <button
              class={styles.logSearchNavBtn}
              onClick={() => emit('search-next')}
              disabled={props.searchMatches.length === 0}
            >
              ›
            </button>
          </span>
        </div>
      )
    }

    // Render tabs
    const renderTabs = () => {
      if (!props.showTabs) return null

      return (
        <div class={styles.headTab}>
          <span
            class={[styles.tabItem, props.activeTab === DETAIL_TAB.LOG && styles.active]}
            onClick={() => emit('tab-change', DETAIL_TAB.LOG)}
          >
            {t('flow.log.log')}
          </span>
          <span
            class={[styles.tabItem, props.activeTab === DETAIL_TAB.SETTING && styles.active]}
            onClick={() => emit('tab-change', DETAIL_TAB.SETTING)}
          >
            {t('flow.log.setting')}
          </span>
        </div>
      )
    }

    // Render more menu
    const renderMoreMenu = () => {
      if (!props.showMoreMenu) return null

      return (
        <ul class={styles.logMoreMenu}>
          {/* 默认菜单项 */}
          {props.showTime !== undefined && (
            <li class={styles.logMoreButton} onClick={() => emit('toggle-time')}>
              {props.showTime ? t('flow.log.hideTime') : t('flow.log.showTime')}
            </li>
          )}
          {props.showDebug !== undefined && (
            <li class={styles.logMoreButton} onClick={() => emit('toggle-debug')}>
              {props.showDebug ? t('flow.log.hideDebugLog') : t('flow.log.showDebugLog')}
            </li>
          )}
          {/* 自定义菜单项 slot */}
          {slots['more-menu']?.()}
          {/* 下载菜单项 */}
          <li class={styles.logMoreButton} onClick={() => emit('download')}>
            {t('flow.log.downloadLog')}
          </li>
        </ul>
      )
    }

    // Render more button
    const renderMoreButton = () => {
      if (!props.showMoreButton) return null

      return (
        <div
          class={styles.logMoreBtnWrapper}
          onClick={(e) => {
            e.stopPropagation()
            emit('toggle-more-menu')
          }}
        >
          <div class={styles.logMoreBtn}>
            <SvgIcon name="more-fill" size={16} />
          </div>
          {props.showMoreMenu && renderMoreMenu()}
        </div>
      )
    }

    return () => (
      <header class={styles.logHead}>
        {/* 标题 */}
        <span class={styles.logTitle}>
          <StatusIcon status={props.status} />
          {props.title}
        </span>

        {/* 标签页 */}
        {renderTabs()}

        {/* 右侧工具栏 */}
        <div class={styles.logTools}>
          {/* 搜索栏 */}
          {props.showSearchBar && renderSearchBar()}

          {/* 更多按钮 */}
          {renderMoreButton()}

          {/* 自定义内容 slot */}
          {slots.tools?.()}
        </div>
      </header>
    )
  },
})

import avatar from '@/assets/avatar.png'
import { Button as BkButton, Divider as BkDivider } from 'bkui-vue'
import { defineComponent, nextTick, PropType, ref } from 'vue'
import { AutoNameIcon, CloseIcon, EditIcon, HistoryIcon, MoreIcon, PlusCircleIcon } from '../Icons'
import styles from './ChatHeader.module.css'

export default defineComponent({
  name: 'ChatHeader',
  props: {
    historyActive: {
      type: Boolean as PropType<boolean>,
      default: false,
    },
    sessionName: {
      type: String as PropType<string>,
      default: '新对话',
    },
  },
  emits: ['new-chat', 'toggle-history', 'close', 'rename'],
  setup(props, { emit }) {
    const menuVisible = ref(false)
    const renaming = ref(false)
    const renameValue = ref('')
    const renameInputRef = ref<HTMLInputElement | null>(null)

    const toggleMenu = () => {
      menuVisible.value = !menuVisible.value
    }

    const closeMenu = () => {
      menuVisible.value = false
    }

    const startRename = () => {
      closeMenu()
      renameValue.value = props.sessionName
      renaming.value = true
      nextTick(() => {
        renameInputRef.value?.focus()
        renameInputRef.value?.select()
      })
    }

    const confirmRename = () => {
      const trimmed = renameValue.value.trim()
      if (trimmed && trimmed !== props.sessionName) {
        emit('rename', trimmed)
      }
      renaming.value = false
    }

    const cancelRename = () => {
      renaming.value = false
    }

    const onRenameKeydown = (e: KeyboardEvent) => {
      if (e.key === 'Enter') {
        e.preventDefault()
        confirmRename()
      } else if (e.key === 'Escape') {
        cancelRename()
      }
    }

    return () => (
      <div class={styles.header}>
        <div class={styles.left}>
          <img class={styles.avatar} src={avatar} alt="avatar" />
          {renaming.value ? (
            <input
              ref={renameInputRef}
              class={styles.renameInput}
              value={renameValue.value}
              onInput={(e: Event) => { renameValue.value = (e.target as HTMLInputElement).value }}
              onBlur={confirmRename}
              onKeydown={onRenameKeydown}
              maxlength={50}
            />
          ) : (
            <>
              <span class={styles.title} title={props.sessionName}>{props.sessionName}</span>
              <div class={styles.moreWrapper}>
                <BkButton text size="small" class={styles.moreBtn} onClick={toggleMenu}>
                  <MoreIcon size={16} />
                </BkButton>
                {menuVisible.value && (
                  <>
                    <div class={styles.menuBackdrop} onClick={closeMenu} />
                    <div class={styles.menu}>
                      <div class={styles.menuItem} onClick={startRename}>
                        <EditIcon size={14} />
                        <span>重命名</span>
                      </div>
                      <div class={[styles.menuItem, styles.menuItemDisabled]}>
                        <AutoNameIcon size={14} />
                        <span>自动生成命名</span>
                      </div>
                    </div>
                  </>
                )}
              </div>
            </>
          )}
        </div>
        <div class={styles.right}>
          <BkButton text size="small" class={styles.iconBtn} title="新对话" onClick={() => emit('new-chat')}>
            <PlusCircleIcon />
          </BkButton>
          <BkButton
            text
            size="small"
            class={[styles.iconBtn, props.historyActive && styles.isActive]}
            title="历史记录"
            onClick={() => emit('toggle-history')}
          >
            <HistoryIcon />
          </BkButton>
          <BkDivider direction="vertical" />
          {/* 数据统计入口暂时隐藏
          <BkButton text size="small" class={styles.iconBtn} title="数据统计">
            <LibraryIcon />
          </BkButton>
          */}
          <BkButton text size="small" class={styles.iconBtn} title="关闭" onClick={() => emit('close')}>
            <CloseIcon />
          </BkButton>
        </div>
      </div>
    )
  },
})

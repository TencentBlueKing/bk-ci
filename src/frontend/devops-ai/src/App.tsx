import { Loading as BkLoading, Sideslider } from 'bkui-vue'
import { defineComponent, ref, Ref, unref } from 'vue'
import styles from './App.module.css'
import ChatHeader from './components/ChatHeader'
import ChatPanel from './components/ChatPanel'
import { TrashIcon } from './components/Icons'
import { useSessionHistory } from './composables'
import { ParentBridgeAction } from './constants'
import './styles/global.css'

interface ChatPanelExposed {
  newChat: () => Promise<void>
  loadSession: (id: string, title?: string) => Promise<void>
  sessionId: Ref<string | null>
  sessionName: Ref<string>
  renameSession: (title: string) => Promise<void>
}

export default defineComponent({
  name: 'App',
  setup() {
    const chatPanelRef = ref<ChatPanelExposed | null>(null)

    const {
      showHistory,
      sessionList,
      historyLoading,
      toggleHistory,
      formatSessionTime,
      deleteSession,
    } = useSessionHistory()

    const loadSession = async (id: string, title?: string) => {
      showHistory.value = false
      chatPanelRef.value?.loadSession(id, title)
    }

    const handleDeleteSession = async (e: MouseEvent, id: string) => {
      e.stopPropagation()
      try {
        await deleteSession(id)
        if (id === unref(chatPanelRef.value?.sessionId)) {
          await chatPanelRef.value?.newChat()
        }
      } catch (err) {
        console.error('Failed to delete session:', err)
      }
    }

    const handleNewChat = () => {
      chatPanelRef.value?.newChat()
    }

    const handleRename = async (title: string) => {
      try {
        await chatPanelRef.value?.renameSession(title)
      } catch (err) {
        console.error('Failed to rename session:', err)
      }
    }

    const closePanel = () => {
      window.parent.postMessage({ action: ParentBridgeAction.ClosePanel }, '*')
    }

    return () => (
      <div class={styles.app}>
        <ChatHeader
          sessionName={chatPanelRef.value?.sessionName ?? '新对话'}
          historyActive={showHistory.value}
          onNew-chat={handleNewChat}
          onToggle-history={toggleHistory}
          onRename={handleRename}
          onClose={closePanel}
        />

        <div class={styles.chatBody}>
          <Sideslider
            v-model:isShow={showHistory.value}
            
            title="历史记录"
            direction="left"
            width={360}
            quickClose
            transfer={false}
            extCls={styles.historySideslider}
          >
            <BkLoading loading={historyLoading.value}>
              <div class={styles.historyPanelBody}>
                {sessionList.value.length === 0 && !historyLoading.value ? (
                  <div class={styles.historyEmpty}>暂无历史记录</div>
                ) : (
                  sessionList.value.map(session => (
                    <div
                      key={session.id}
                      class={[
                        styles.historyItem,
                        session.id === unref(chatPanelRef.value?.sessionId) && styles.historyItemActive,
                      ]}
                      onClick={() => loadSession(session.id, session.title)}
                    >
                      <div class={styles.historyItemTitle}>{session.title}</div>
                      <div class={styles.historyItemMeta}>
                        <span class={styles.historyItemTime}>{formatSessionTime(session.updatedAt)}</span>
                        <button
                          type="button"
                          class={styles.historyItemDelete}
                          title="删除"
                          aria-label="删除会话"
                          onClick={(e: MouseEvent) => handleDeleteSession(e, session.id)}
                        >
                          <TrashIcon size={14} />
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </BkLoading>
          </Sideslider>

          <ChatPanel ref={chatPanelRef} />
        </div>
      </div>
    )
  },
})

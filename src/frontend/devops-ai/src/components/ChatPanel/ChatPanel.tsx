
import { ChatContainer, MessageRole } from '@blueking/chat-x'
import { defineComponent, onBeforeUnmount, onMounted, nextTick } from 'vue'
import { ParentBridgeAction } from '../../constants'
import { useChat, useResources } from '../../composables'
import { useParams } from '../../composables/useParams'
import { ActivityMessage } from '../ActivityMessage'
import { InterruptMessage } from '../InterruptMessage'
import { SceneAttachment } from '../SceneAttachment'
import { StructuredMessageRender } from '../StructuredBlocks'
import { SubAgentMessage } from '../SubAgentMessage'
import WelcomeGuide from '../WelcomeGuide'

export default defineComponent({
  name: 'ChatPanel',
  setup(_props, { expose }) {
    const { setVersion } = useParams()

    const { prompts, resources, shortcuts, loadPrompts, loadResources } =
      useResources()

    const {
      sessionId,
      sessionName,
      messages,
      messageStatus,
      chatLoading,
      userInput,
      initSession,
      queueAutoSend,
      sendMessage,
      stopSending,
      resumeInterrupt,
      dismissInterrupt,
      selectShortcut,
      deleteShortcut,
      closeShortcut,
      submitShortcut,
      newChat,
      loadSession,
      renameSession,
    } = useChat(resources)

    let disposed = false
    let lastAnalysisNonce = ''

    function requestPendingAnalysis() {
      if (!window.parent || window.parent === window) return
      window.parent.postMessage({ action: ParentBridgeAction.RequestPendingAnalysis }, '*')
    }

    function ackAnalysis(nonce?: string) {
      if (!window.parent || window.parent === window) return
      window.parent.postMessage({
        action: ParentBridgeAction.AckAnalysis,
        params: { nonce },
      }, '*')
    }

    onMounted(() => {
      void initSession()
      loadPrompts()
      loadResources()
    })

    expose({ newChat, loadSession, sessionId, sessionName, renameSession })

    function fillInputCallback(data: any) {
      userInput.value = data
    }

    

    function handleAnalysisRequest(event: MessageEvent) {
      if (event.data?.action !== ParentBridgeAction.RequestAnalysis || event.source !== window.parent) return
      const nonce = event.data?.params?.nonce
      if (nonce && nonce === lastAnalysisNonce) {
        ackAnalysis(nonce)
        return
      }
      if (nonce) lastAnalysisNonce = String(nonce)
      const contextVersion = event.data?.params?.context?.version
      setVersion(contextVersion == null ? '' : String(contextVersion))
      const prompt = event.data?.params?.prompt
      if (!prompt) return
      ackAnalysis(nonce)
      const autoSend = event.data?.params?.autoSend !== false
      const attachment = event.data?.params?.attachment
      const sceneAttachment = attachment?.title
        ? {
            title: String(attachment.title),
            body: String(attachment.body || ''),
          }
        : undefined
      userInput.value = autoSend ? '' : prompt
      if (!autoSend) {
        void nextTick(() => {
          const input = document.querySelector('.ai-slash-input') as HTMLElement | null
          input?.focus()
        })
        return
      }
      if (disposed) return
      queueAutoSend({
        prompt,
        extraContext: sceneAttachment
          ? [{ description: sceneAttachment.title, value: sceneAttachment.body }]
          : [],
        sceneAttachment,
      })
    }

    window.addEventListener('message', handleAnalysisRequest)
    requestPendingAnalysis()

    onBeforeUnmount(() => {
      disposed = true
      window.removeEventListener('message', handleAnalysisRequest)
    })

    return () => (
      <ChatContainer
        v-model={userInput.value}
        messages={messages.value}
        chatLoading={chatLoading.value}
        messageStatus={messageStatus.value}
        prompts={prompts.value}
        resources={resources.value}
        onSendMessage={sendMessage}
        onStopSending={stopSending}
        shortcuts={shortcuts.value}
        placeholder="输入问题，/ 唤出提示词，@ 选择技能或工具"
        onDeleteShortcut={deleteShortcut}
        onSelectShortcut={selectShortcut}
        onShortcutClose={closeShortcut}
        onShortcutSubmit={submitShortcut}
        onStopStreaming={stopSending}
        
        v-slots={{
          welcome: () => (
            <WelcomeGuide
              onSendMessage={sendMessage}
              onFillInput={fillInputCallback}
            />
          ),
          message: ({ message, messageToolsStatus }: any) => {
            
            if (message.role === MessageRole.Pause && message.interrupt) {
              return (
                <InterruptMessage
                  message={message}
                  onApprove={(payload: any) => resumeInterrupt(payload)}
                  onReject={() => dismissInterrupt()}
                />
              )
            }
            if (message.role === MessageRole.Activity && message.activityType) {
              return <ActivityMessage message={message} />
            }
            if (message.agentName) {
              return <SubAgentMessage message={message} />
            }
            const rendered = (
              <StructuredMessageRender
                message={message}
                messageToolsStatus={messageToolsStatus}
                onSendMessage={sendMessage}
              />
            )
            if (message.role === MessageRole.User && message.sceneAttachment) {
              return (
                <>
                  {rendered}
                  <SceneAttachment
                    title={message.sceneAttachment.title}
                    body={message.sceneAttachment.body}
                  />
                </>
              )
            }
            return rendered
          },
        }}
      />
    )
  },
})

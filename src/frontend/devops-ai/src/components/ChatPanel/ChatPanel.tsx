
import { ChatContainer, MessageRole } from '@blueking/chat-x'
import { defineComponent, onMounted } from 'vue'
import { useChat, useResources } from '../../composables'
import { useStickyUserMessage } from '../../composables/useStickyUserMessage'
import { ActivityMessage } from '../ActivityMessage'
import { InterruptMessage } from '../InterruptMessage'
import { StructuredMessageRender } from '../StructuredBlocks'
import { SubAgentMessage } from '../SubAgentMessage'
import WelcomeGuide from '../WelcomeGuide'

export default defineComponent({
  name: 'ChatPanel',
  setup(_props, { expose }) {

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

    useStickyUserMessage(messages, chatLoading)

    onMounted(() => {
      initSession()
      loadPrompts()
      loadResources()
    })

    expose({ newChat, loadSession, sessionId, sessionName, renameSession })

    function fillInputCallback(data: any) {
      userInput.value = data
    }

    

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
            return (
              <StructuredMessageRender
                message={message}
                messageToolsStatus={messageToolsStatus}
                onSendMessage={sendMessage}
              />
            )
          },
        }}
      />
    )
  },
})

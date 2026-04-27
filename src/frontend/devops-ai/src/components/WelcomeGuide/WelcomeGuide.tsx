import {
  Button as BkButton,
  Dialog as BkDialog,
  Form as BkForm,
  Input as BkInput,
  Loading as BkLoading,
} from 'bkui-vue'
import BkCollapse from 'bkui-vue/lib/collapse'
import { defineComponent, reactive, ref } from 'vue'
import bannerSvg from '../../assets/aiblukeing-banner.svg'
import { buildFormCollectPrompt, CardAction, useWelcomeGuide } from '../../composables'
import { ArrowRightIcon, EditIcon, RefreshIcon } from '../Icons'
import styles from './WelcomeGuide.module.css'

const BkCollapsePanel = BkCollapse.CollapsePanel
const { FormItem: BkFormItem } = BkForm

export default defineComponent({
  name: 'WelcomeGuide',
  emits: ['sendMessage', 'fillInput'],
  setup(_props, { emit }) {
    const { cards, hotQuestions, loading, isRefreshing, refreshHotQuestions } =
      useWelcomeGuide()
    const initExpandedIndex = ref<number[]>([0])

    const formVisible = ref(false)
    const formRef = ref<{ validate?: () => Promise<void> } | null>(null)
    const formAction = ref<CardAction | null>(null)
    const formModel = reactive<Record<string, string>>({})

    const resetFormState = () => {
      formAction.value = null
      Object.keys(formModel).forEach((k) => {
        delete formModel[k]
      })
    }

    const openFormCollect = (action: CardAction) => {
      const fields = action.formFields || []
      if (!fields.length) {
        send(action.prompt)
        return
      }
      resetFormState()
      formAction.value = action
      for (const f of fields) {
        formModel[f.key] = ''
      }
      formVisible.value = true
    }

    const onDialogClosed = () => {
      formVisible.value = false
      resetFormState()
    }

    const handleFormConfirm = async () => {
      const action = formAction.value
      if (!action) return
      try {
        await formRef.value?.validate?.()
      } catch {
        return
      }
      const prompt = buildFormCollectPrompt(action, { ...formModel })
      emit('sendMessage', prompt)
      formVisible.value = false
    }

    const handleAction = (action: CardAction) => {
      console.log('handleAction', action)
      if (['PROMPT_COMPLETION'].includes(action.interactionType)) {
        emit('fillInput', action.prompt)
      } else if (['FORM_COLLECT'].includes(action.interactionType)) {
        openFormCollect(action)
      } else {
        send(action.prompt)
      }
    }

    const send = (prompt: string) => emit('sendMessage', prompt)

    return () => (
      <div class={styles.root}>
        <BkDialog
          isShow={formVisible.value}
          title={formAction.value?.label || '补充信息'}
          width={520}
          onClosed={onDialogClosed}
        >
          {{
            default: () => (
              <div class={styles.formDialogBody}>
                <BkForm ref={formRef} model={formModel} labelWidth={100}>
                  {(formAction.value?.formFields || []).map((field) => (
                    <BkFormItem
                      key={field.key}
                      label={field.label}
                      property={field.key}
                      required={field.required}
                    >
                      {field.type === 'textarea' ? (
                        <BkInput
                          type="textarea"
                          rows={4}
                          v-model={formModel[field.key]}
                          placeholder={field.placeholder}
                        />
                      ) : (
                        <BkInput
                          v-model={formModel[field.key]}
                          placeholder={field.placeholder}
                        />
                      )}
                    </BkFormItem>
                  ))}
                </BkForm>
              </div>
            ),
            footer: () => (
              <>
                <BkButton onClick={() => { formVisible.value = false }}>取消</BkButton>
                <BkButton theme="primary" onClick={handleFormConfirm}>
                  发送
                </BkButton>
              </>
            ),
          }}
        </BkDialog>
        <div class={styles.banner}>
          <img src={bannerSvg} alt="AI 蓝鲸" class={styles.bannerImg} />
        </div>
        <h1 class={styles.title}>你好，欢迎使用蓝盾智能助手</h1>
        <p class={styles.subtitle}>我可以协助你完成如下事项：</p>

        <div class={styles.content}>
          <BkLoading loading={loading.value}>
            <BkCollapse
              v-model={initExpandedIndex.value}
              class={styles.capabilities}
              headerIconAlign="right"
              useBlockTheme
            >
              {cards.value.map((cap, idx) => (
                <BkCollapsePanel
                  key={idx}
                  name={idx}
                  title={cap.label}
                  v-slots={{
                    content: () => (
                      <>
                        <p class={styles.panelDesc}>{cap.description}</p>
                        <div class={styles.panelActions}>
                          {cap.actions.map((action, i) => (
                            <BkButton
                              key={i}
                              outline
                              size="small"
                              hoverTheme="primary"
                              class={styles.actionBtn}
                              onClick={(e) => { e.stopPropagation(); handleAction(action) }}
                            >
                              {action.label}
                              {action.interactionType === 'PROMPT_COMPLETION' && (
                                <EditIcon size={12} class={styles.actionBtnIcon} />
                              )}
                            </BkButton>
                          ))}
                        </div>
                      </>
                    ),
                  }}
                />
              ))}
            </BkCollapse>

            {hotQuestions.value.length > 0 && (
              <div class={styles.askSection}>
                <p class={styles.askLabel}>
                  试试这样问：
                  <span
                    class={[styles.refreshBtn, isRefreshing.value && styles.isSpinning]}
                    onClick={refreshHotQuestions}
                  >
                    <RefreshIcon />
                  </span>
                </p>
                <div class={styles.suggestions}>
                  {hotQuestions.value.map((question: string, idx: number) => (
                    <div
                      key={idx}
                      class={styles.suggestionCard}
                      onClick={() => send(question)}
                    >
                      <span>{question}</span>
                      <ArrowRightIcon />
                    </div>
                  ))}
                </div>
              </div>
            )}
          </BkLoading>
        </div>
      </div>
    )
  },
})

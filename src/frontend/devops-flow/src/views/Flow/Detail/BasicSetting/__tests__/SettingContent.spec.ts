import { createApp, defineComponent, h, nextTick, ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import type { FlowSettings } from '@/api/flowModel'
import { RunLockType } from '@/types/flow'
import SettingContent from '../SettingContent'

vi.mock('vue-i18n', () => ({ useI18n: () => ({ t: (key: string) => key }) }))
vi.mock('vue-router', () => ({ useRoute: () => ({ params: { flowId: 'test-flow' } }) }))
vi.mock('bkui-vue', () => ({
  Collapse: defineComponent({
    props: ['list'],
    setup(props, { slots }) {
      return () =>
        h(
          'section',
          props.list.map((item: unknown) => slots.content?.(item)),
        )
    },
  }),
}))

const makeSettings = (overrides: Partial<FlowSettings> = {}): FlowSettings => ({
  pipelineName: 'test-flow',
  desc: '',
  runLockType: RunLockType.MULTIPLE,
  maxConRunningQueueSize: 40,
  waitQueueTimeMinute: 20,
  ...overrides,
})

function mountSettings(basicSettings: FlowSettings) {
  const settings = ref(basicSettings)
  const element = document.createElement('div')
  const app = createApp(() => h(SettingContent, { basicSettings: settings.value }))
  app.mount(element)
  return {
    text: () => element.textContent ?? '',
    findAll: (selector: string) => element.querySelectorAll(selector),
    setProps: async (props: { basicSettings: FlowSettings }) => {
      settings.value = props.basicSettings
      await nextTick()
    },
    unmount: () => app.unmount(),
  }
}

describe('read-only basic settings runtime variable limit policy', () => {
  it.each([
    [true, 'flow.variable.failOversized'],
    [false, 'flow.variable.discardOversized'],
    [undefined, 'flow.variable.discardOversized'],
    [null, 'flow.variable.discardOversized'],
  ])('renders policy %s with the editor-compatible fallback', (value, expected) => {
    const wrapper = mountSettings(
      makeSettings({ failIfVariableInvalid: value as boolean | undefined }),
    )
    expect(wrapper.text()).toContain('flow.variable.limitPolicy')
    expect(wrapper.text()).toContain(expected)
    expect(wrapper.findAll('input, button, textarea, select')).toHaveLength(0)
    wrapper.unmount()
  })

  it('updates the displayed policy when the selected version changes', async () => {
    const wrapper = mountSettings(makeSettings({ failIfVariableInvalid: false }))
    await wrapper.setProps({ basicSettings: makeSettings({ failIfVariableInvalid: true }) })
    expect(wrapper.text()).toContain('flow.variable.failOversized')
    expect(wrapper.text()).not.toContain('flow.variable.discardOversized')
    await wrapper.setProps({ basicSettings: makeSettings() })
    expect(wrapper.text()).toContain('flow.variable.discardOversized')
    wrapper.unmount()
  })

  it.each([RunLockType.MULTIPLE, RunLockType.GROUP_LOCK])(
    'keeps the policy visible alongside concurrency mode %s',
    (runLockType) => {
      const wrapper = mountSettings(
        makeSettings({
          runLockType,
          failIfVariableInvalid: true,
          concurrencyGroup: 'test-group',
          concurrencyCancelInProgress: false,
          maxQueueSize: 10,
        }),
      )
      expect(wrapper.text()).toContain('flow.variable.failOversized')
      expect(wrapper.text()).toContain('flow.content.concurrencySettings')
      expect(wrapper.text()).toContain(runLockType === RunLockType.MULTIPLE ? '40' : 'test-group')
      wrapper.unmount()
    },
  )
})

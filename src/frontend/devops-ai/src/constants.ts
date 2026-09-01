export const CustomEventName = {
  SubAgent: 'subagent_event',
  ActivitySnapshot: 'activity_snapshot',
} as const

export const ServerRole = {
  User: 'USER',
  Assistant: 'ASSISTANT',
  System: 'SYSTEM',
  Tool: 'TOOL',
  Reasoning: 'REASONING',
} as const

export const ActivityType = {
  DataTable: 'data_table',
  KeyValue: 'key_value',
  GroupedList: 'grouped_list',
  OperationResult: 'operation_result',
} as const

/**
 * AI iframe ↔ devops-nav 父窗口的 postMessage 协议。
 * 这些字符串需要与 devops-nav `AI_IFRAME_ACTIONS` 保持一致。
 */
export const ParentBridgeAction = {
  /** 父窗口推送当前标签页的项目/流水线/构建上下文给本 iframe。 */
  SyncContext: 'syncAiContext',
  /** 本 iframe 主动请求父窗口重发一次最新上下文。 */
  RequestContext: 'requestAiContext',
  /** 本 iframe 通知父窗口关闭 AI 面板。 */
  ClosePanel: 'CLOSE_DEVOPS_AI_PANEL',
} as const

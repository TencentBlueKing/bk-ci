const WEWORK_NOTIFY_TYPES = new Set(['WEWORK', 'RTX'])

export const NOTIFY_TYPE_LABEL_KEY: Record<string, string> = {
  EMAIL: 'flow.content.emailNotice',
  WEWORK: 'flow.content.weworkNotice',
  RTX: 'flow.content.weworkNotice',
  WEWORK_GROUP: 'flow.content.weworkGroup',
  VOICE: 'flow.content.voiceNotice',
  WECHAT: 'flow.content.wechatNotice',
  SMS: 'flow.content.smsNotice',
}

export function matchesNotifyType(optionId: string, storedType: string): boolean {
  if (optionId === 'WEWORK') return WEWORK_NOTIFY_TYPES.has(storedType)
  return optionId === storedType
}

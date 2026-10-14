export type AutoSendPayload = {
  prompt: string
  extraContext?: { description: string; value: string }[]
  sceneAttachment?: { title: string; body: string }
}

export function createAutoSendGate() {
  let loading = false
  let generation = 0
  let pending: AutoSendPayload | null = null

  return {
    beginInit() {
      generation += 1
      loading = true
      return generation
    },
    endInit(gen: number) {
      if (gen !== generation) return false
      loading = false
      return true
    },
    queue(payload: AutoSendPayload) {
      pending = payload
    },
    isCurrent(gen: number) {
      return gen === generation
    },
    take() {
      if (loading || !pending) return null
      const payload = pending
      pending = null
      return payload
    },
  }
}

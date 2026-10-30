export interface RealtimeHandle {
    refresh(type?: string): void
    dispose(): void
}

export interface WatchRealtimeOptions {
    topics?: string[]
    load?: () => unknown
    apply?: (data: unknown) => void
    onError?: (error: unknown) => void
    onForbidden?: (topic: string) => void
    interval?: number
    liveInterval?: number
    pollingInterval?: number
}

export function subscribeRealtime(
    topics: string[],
    callback: (type: string, topic?: string) => void,
): () => void

export function watchRealtime(options: WatchRealtimeOptions): RealtimeHandle

export function installRealtimeHost(options: {
    getFrame: () => HTMLIFrameElement | null
    onActive?: () => void
    onIdle?: () => void
}): {
    reset: () => void
    dispose: () => void
}

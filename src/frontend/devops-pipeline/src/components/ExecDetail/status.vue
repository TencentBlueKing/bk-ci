<template>
    <span class="log-status readonly">
        <logo
            v-if="isRunning"
            name="circle-2-1"
            :size="16"
            class="executing running"
            :style="outlineColorStyle"
            aria-hidden="true"
        />
        <logo
            v-else-if="outlineIconName"
            :name="outlineIconName"
            :size="16"
            :style="outlineColorStyle"
            aria-hidden="true"
        />
        <logo
            v-else-if="statusIconAttrs"
            v-bind="statusIconAttrs"
        />
        <logo
            v-else
            name="circle"
            :size="16"
            class="empty-status-icon pending"
            aria-hidden="true"
        />
    </span>
</template>

<script>
    import Logo from '@/components/Logo'
    import { statusColorMap, statusIconMap } from '@/utils/pipelineStatus'

    const IDLE_COLOR_ON_DARK = '#63656e'
    const HOOK_STATUSES = ['SUCCEED', 'FAILED', 'HEARTBEAT_TIMEOUT', 'QUEUE_TIMEOUT', 'EXEC_TIMEOUT']

    export default {
        name: 'log-status',
        components: {
            Logo
        },
        props: {
            status: {
                type: String,
                default: ''
            },
            isHook: Boolean,
            outline: Boolean
        },
        computed: {
            outlineIconName () {
                if (!this.outline) return ''
                const status = this.status || 'UNEXEC'
                if (!statusIconMap[status]) return ''
                return this.isHook && HOOK_STATUSES.includes(status) ? 'build-hooks' : statusIconMap[status]
            },
            outlineColorStyle () {
                if (!this.outline) return null
                const color = statusColorMap[this.status || 'UNEXEC']
                if (!color) return null
                return { color: color === statusColorMap.UNEXEC ? IDLE_COLOR_ON_DARK : color }
            },
            isRunning () {
                return [
                    'RUNNING',
                    'PREPARE_ENV',
                    'DEPENDENT_WAITING',
                    'LOOP_WAITING',
                    'CALL_WAITING'
                ].includes(this.status)
            },
            statusIconAttrs () {
                switch (this.status) {
                    case 'WAITING':
                        return {
                            name: 'build-waiting',
                            size: 16,
                            class: 'pending'
                        }
                    case 'QUEUE':
                    case 'QUEUE_CACHE':
                        return {
                            name: 'hourglass',
                            size: 16,
                            class: 'queue'
                        }
                    case 'REVIEW_ABORT':
                    case 'REVIEWING':
                        return {
                            name: 'build-warning',
                            size: 16,
                            class: 'warning'
                        }
                    case 'FAILED':
                    case 'HEARTBEAT_TIMEOUT':
                    case 'QUEUE_TIMEOUT':
                    case 'EXEC_TIMEOUT':
                        return {
                            name: this.isHook ? 'build-hooks' : 'close-circle-shape',
                            size: 16,
                            class: 'danger'
                        }
                    case 'SUCCEED':
                        return {
                            name: this.isHook ? 'build-hooks' : 'check-circle-shape',
                            size: 16,
                            class: 'success'
                        }
                    case 'PAUSE':
                        return {
                            name: 'build-pause',
                            size: 16,
                            class: 'pause'
                        }
                    case 'SKIP':
                        return {
                            name: 'build-canceled',
                            size: 16,
                            class: 'pending'
                        }
                    case 'CANCELED':
                    case 'TERMINATE':
                        return {
                            name: 'abort',
                            size: 16,
                            class: 'canceled'
                        }
                    default:
                        return null
                }
            }

        }
    }
</script>

<style lang="scss" scoped>
    .log-status {
        position: relative;
        text-align: center;
        overflow: hidden;
        font-size: 16px;
        width: 42px;
        height: 42px;
        line-height: 42px;
        box-sizing: border-box;
        padding: 12px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        svg { width: 16px; height: 16px; vertical-align: middle; }

        > span,
        > i {
            position: absolute;
            width: 100%;
            height: 100%;
            line-height: inherit;
            left: 0;
            top: 0;
            transition: all .3s cubic-bezier(1.0, 0.5, 0.8, 1.0);
        }

        &:not(.readonly) {
            background-color: #3c96ff;
            > span,
            > i {
                background-color: #3c96ff;
            }
            .warning {
                background-color: #ffb400;
                color: white;
            }
            .danger {
                background-color: #ff5656;
                color: white;
            }
            .success {
                background-color: #34d97b;
                color: white;
            }
        }
        .warning, .queue, .pause, .canceled { color: #f0aa50; }
        .pending { color: #83828c; }
        .danger { color: #f06e6e; }
        .success { color: #5ac882; }
        .executing { animation: rotating infinite .6s ease-in-out; }

        @media (prefers-reduced-motion: reduce) {
            .executing { animation: none; }
        }

        &.readonly {
            font-size: 12px;
            font-weight: normal;
            background-color: transparent;
            &.container {
                color: white;
            }
        }
    }
</style>

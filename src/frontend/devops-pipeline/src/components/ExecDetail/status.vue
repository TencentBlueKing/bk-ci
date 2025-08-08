<template>
    <span class="log-status readonly">
        <i
            v-if="isRunning"
            class="devops-icon icon-circle-2-1 executing"
        />
        <logo
            v-else
            v-bind="statusIconAttrs"
        />
    </span>
</template>

<script>
    import Logo from '@/components/Logo'
    export default {
        name: 'log-status',
        components: {
            Logo
        },
        props: {
            status: {
                type: String,
                default: 'CANCELED'
            },
            isHook: Boolean
        },
        computed: {
            isRunning () {
                return [
                    'RUNNING',
                    'PREPARE_ENV',
                    'QUEUE',
                    'LOOP_WAITING',
                    'CALL_WAITING'
                ].includes(this.status)
            },
            statusIconAttrs () {
                switch (this.status) {
                    case 'WAITING':
                        return {
                            name: 'build-waiting'
                        }
                    case 'REVIEW_ABORT':
                    case 'REVIEWING':
                        return {
                            name: 'build-warning'
                        }
                    case 'FAILED':
                    case 'HEARTBEAT_TIMEOUT':
                    case 'QUEUE_TIMEOUT':
                    case 'EXEC_TIMEOUT':
                        return {
                            name: `build-${this.isHook ? 'hooks' : 'failed'}`,
                            class: 'danger'
                        }
                    case 'SUCCEED':
                        return {
                            name: `build-${this.isHook ? 'hooks' : 'sucess'}`,
                            class: 'success'
                        }
                    case 'PAUSE':
                        return {
                            name: 'build-pause',
                            class: 'pause'
                        }
                    case 'SKIP':
                    case 'CANCELED':
                    case 'TERMINATE':
                    default:
                        return {
                            fill: '#fff',
                            name: 'build-canceled'
                        }
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
        svg {
            width: 18px;
            height: 18px;
            vertical-align: top;
        }

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
        .warning {
            color: #ffb400;
        }
        .danger {
            color: #ff5656;
        }
        .success {
            color: #34d97b;
        }
        .executing {
            &:before {
                display: inline-block;
                animation: rotating infinite .6s ease-in-out;
            }
        }

        &.readonly {
            font-size: 12px;
            font-weight: normal;
            background-color: transparent;
            &.container {
                > span,
                > i {
                    color: white;
                }
            }
        }
    }
</style>

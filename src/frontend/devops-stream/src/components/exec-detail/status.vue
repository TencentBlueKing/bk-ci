<template>
    <span class="log-status readonly">
        <i v-if="status === 'RUNNING' || status === 'PREPARE_ENV' || status === 'QUEUE' || status === 'LOOP_WAITING' || status === 'CALL_WAITING'" class="bk-icon icon-circle-2-1 executing" />
        <svg aria-hidden="true" v-if="status === 'WAITING'">
            <use xlink:href="#stream-build-waiting"></use>
        </svg>
        <svg fill="#fff" aria-hidden="true" v-if="['SKIP', 'CANCELED', 'TERMINATE'].includes(status) || !status">
            <use xlink:href="#stream-build-canceled"></use>
        </svg>
        <svg aria-hidden="true" v-if="['REVIEW_ABORT', 'REVIEWING'].includes(status)">
            <use xlink:href="#stream-build-warning"></use>
        </svg>
        <svg aria-hidden="true" v-if="['FAILED', 'HEARTBEAT_TIMEOUT', 'QUEUE_TIMEOUT', 'EXEC_TIMEOUT'].includes(status)" class="danger">
            <use xlink:href="#stream-build-hooks" v-if="isHook"></use>
            <use xlink:href="#stream-build-failed" v-else></use>
        </svg>
        <svg aria-hidden="true" v-if="status === 'SUCCEED'" class="success">
            <use xlink:href="#stream-build-hooks" v-if="isHook"></use>
            <use xlink:href="#stream-build-sucess" v-else></use>
        </svg>
        <svg aria-hidden="true" v-if="status === 'PAUSE'" class="pause">
            <use xlink:href="#stream-build-pause"></use>
        </svg>
    </span>
</template>

<script>
    export default {
        name: 'LogStatus',
        props: {
            status: {
                type: String,
                default: 'CANCELED'
            },
            isHook: Boolean
        }
    }
</script>

<style lang="postcss" scoped>
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

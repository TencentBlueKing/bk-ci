<template>
    <span class="log-status readonly">
        <i v-if="status === 'RUNNING' || status === 'PREPARE_ENV' || status === 'QUEUE' || status === 'LOOP_WAITING' || status === 'CALL_WAITING'" class="bk-icon icon-circle-2-1 executing" />
        <i v-if="status === 'WAITING'" class="bk-icon icon-clock" />
        <i v-if="status === 'CANCELED'" class="bk-icon warning icon-exclamation-circle-shape" />
        <i v-if="status === 'REVIEWING' || status === 'REVIEW_ABORT'" class="bk-icon warning icon-exclamation-triangle-shape" />
        <i v-if="status === 'FAILED' || status === 'HEARTBEAT_TIMEOUT' || status === 'QUEUE_TIMEOUT' || status === 'EXEC_TIMEOUT'" class="bk-icon danger icon-close-circle-shape" />
        <i v-if="status === 'SUCCEED'" class="bk-icon success icon-check-circle-shape" />
    </span>
</template>

<script>
    export default {
        name: 'log-status',
        props: {
            status: String
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

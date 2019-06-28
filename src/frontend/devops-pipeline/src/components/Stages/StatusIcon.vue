<template>
    <span :class="{ 'stage-status': true, [type]: type, 'readonly': !editable || (jobOption && jobOption.enable === false) }">
        <transition name="slide-top">
            <i v-if="status === 'RUNNING' || status === 'PREPARE_ENV' || status === 'QUEUE' || status === 'LOOP_WAITING' || status === 'CALL_WAITING'"
                class="bk-icon icon-circle-2-1 executing-job" />
        </transition>
        <transition name="slide-top">
            <i v-if="status === 'WAITING'" class="bk-icon icon-clock" />
        </transition>
        <transition name="slide-down">
            <i v-if="status === 'CANCELED'" class="bk-icon warning icon-exclamation-circle-shape" />
        </transition>
        <transition name="slide-down">
            <i v-if="status === 'REVIEWING' || status === 'REVIEW_ABORT'" class="bk-icon warning icon-exclamation-triangle-shape" />
        </transition>
        <transition name="slide-left">
            <i v-if="status === 'FAILED' || status === 'HEARTBEAT_TIMEOUT' || status === 'QUEUE_TIMEOUT' || status === 'EXEC_TIMEOUT'"
                class="bk-icon danger icon-close-circle-shape" />
        </transition>
        <transition name="slide-right">
            <i v-if="status === 'SUCCEED'" class="bk-icon success icon-check-circle-shape" />
        </transition>
        <slot v-if="!status || status === 'SKIP'"></slot>
    </span>
</template>

<script>
    export default {
        name: 'stage-status',
        props: {
            status: String,
            type: String,
            editable: Boolean,
            serialNum: String,
            jobOption: Object
        }
    }
</script>

<style lang="scss">
@import "./Stage";
.stage-status {
    position: relative;
    text-align: center;
    overflow: hidden;
    font-size: 16px;
    width: $serialSize;
    height: $serialSize;
    line-height: $serialSize;
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
        background-color: $primaryColor;
        > span,
        > i {
            background-color: $primaryColor;
        }
        .warning {
            background-color: $warningColor;
            color: white;
        }
        .danger {
            background-color: $dangerColor;
            color: white;
        }
        .success {
            background-color: $successColor;
            color: white;
        }
    }
    .warning {
       color: $warningColor;
    }
    .danger {
        color: $dangerColor;
    }
    .success {
        color: $successColor;
    }
    .executing-job {
        &:before {
            display: inline-block;
            animation: rotating infinite .6s ease-in-out;
        }
    }

    .slide-top-enter, .slide-top-leave-to {
        transform: translateY(42px);
    }
    .slide-down-enter, .slide-down-leave-to {
        transform: translateY(-42px);
    }
    .slide-left-enter, .slide-left-leave-to {
        transform: translateX(42px);
    }
    .slide-right-enter, .slide-right-leave-to {
        transform: translateX(-42px);
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

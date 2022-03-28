<template>
    <span :class="{ 'stage-status': true, [type]: type, 'readonly': !editable || containerDisabled }">
        <transition name="slide-top">
            <i v-if="status === 'RUNNING' || status === 'PREPARE_ENV' || status === 'QUEUE' || status === 'LOOP_WAITING' || status === 'CALL_WAITING'"
                class="devops-icon icon-circle-2-1 executing-job" />
            <i v-if="status === 'DEPENDENT_WAITING'" v-bk-tooltips="dependOnValue"
                class="devops-icon icon-hourglass" />
        </transition>
        <transition name="slide-top">
            <i v-if="status === 'WAITING' || status === 'UNEXEC'" class="devops-icon icon-clock" />
        </transition>
        <transition name="slide-down">
            <i v-if="status === 'CANCELED' || status === 'TERMINATE'" class="devops-icon warning icon-exclamation-circle-shape" />
        </transition>
        <transition name="slide-down">
            <i v-if="status === 'REVIEWING' || status === 'REVIEW_ABORT'" class="devops-icon warning icon-exclamation-triangle-shape" />
        </transition>
        <transition name="slide-left">
            <template v-if="status === 'FAILED' || status === 'HEARTBEAT_TIMEOUT' || status === 'QUEUE_TIMEOUT' || status === 'EXEC_TIMEOUT'">
                <logo size="12" v-if="isHook" name="hooks" class="danger"></logo>
                <i v-else class="devops-icon danger icon-close-circle-shape" />
            </template>
        </transition>
        <transition name="slide-right">
            <template v-if="status === 'SUCCEED'">
                <logo size="12" v-if="isHook" name="hooks" class="success"></logo>
                <i v-else class="devops-icon success icon-check-circle-shape" />
            </template>
        </transition>
        <transition name="slide-right">
            <logo name="pause" size="12" v-if="status === 'PAUSE'" class="status-logo pause"></logo>
        </transition>
    </span>
</template>

<script>
    import Logo from '@/components/Logo'

    export default {
        name: 'stage-status',
        components: {
            Logo
        },
        props: {
            status: String,
            type: String,
            editable: Boolean,
            serialNum: String,
            containerDisabled: Boolean,
            dependOnValue: String,
            isHook: Boolean
        }
    }
</script>

<style lang="scss" scoped>
@import "./Stage";
.stage-status {
    position: relative;
    text-align: center;
    overflow: hidden;
    font-size: 14px;
    width: 20px;
    height: 20px;
    line-height: 20px;
    box-sizing: border-box;

    .status-logo {
        position: absolute;
        left: 15px;
        top: 15px;
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
    .pause {
        color: $pauseColor;
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
}
</style>

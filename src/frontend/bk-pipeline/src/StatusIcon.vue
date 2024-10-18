<template>
    <span :class="{ 'stage-status': true, [type]: type, [status]: status, 'readonly': !editable || containerDisabled }">
        <transition name="slide-top">
            <Logo
                size="14"
                v-if="isLoading"
                name="circle-2-1"
                class="spin-icon"
            />
            <Logo
                size="14"
                v-else-if="isHangon"
                v-bk-tooltips="dependOnValue"
                name="hourglass"
            />
        </transition>
        <transition name="slide-top">
            <Logo
                size="14"
                v-if="isWaiting"
                name="clock"
            />
        </transition>
        <transition name="slide-down">
            <Logo
                size="14"
                v-if="isCancel"
                name="exclamation-circle-shape"
            />
        </transition>
        <transition name="slide-down">
            <Logo
                size="14"
                v-if="isReviewing"
                name="exclamation-triangle-shape"
            />
        </transition>
        <transition name="slide-left">
            <Logo
                :size="isHook ? 18 : 14"
                v-if="isFailed"
                :name="isHook ? 'build-hooks' : 'close-circle-shape'"
            />
        </transition>
        <transition name="slide-right">
            <Logo
                :size="isHook ? 18 : 14"
                v-if="isSucceed"
                :name="isHook ? 'build-hooks' : 'check-circle-shape'"
            ></Logo>
        </transition>
        <transition name="slide-right">
            <Logo
                size="16"
                name="pause"
                v-if="isPause"
            ></Logo>
        </transition>
        <span v-if="defaultStatus">
            <slot></slot>
        </span>
    </span>
</template>

<script>
    import Logo from './Logo'
    import { STATUS_MAP } from './constants'
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
        },
        computed: {
            isLoading () {
                return [
                    STATUS_MAP.RUNNING,
                    STATUS_MAP.PREPARE_ENV,
                    STATUS_MAP.QUEUE,
                    STATUS_MAP.LOOP_WAITING,
                    STATUS_MAP.CALL_WAITING
                ].includes(this.status)
            },
            isHangon () {
                return [STATUS_MAP.DEPENDENT_WAITING].includes(this.status)
            },
            isWaiting () {
                return [STATUS_MAP.WAITING, STATUS_MAP.UNEXEC].includes(this.status)
            },
            isCancel () {
                return [STATUS_MAP.TERMINATE, STATUS_MAP.CANCELED].includes(this.status)
            },
            isReviewing () {
                return [STATUS_MAP.REVIEWING, STATUS_MAP.REVIEW_ABORT].includes(this.status)
            },
            isFailed () {
                return [
                    STATUS_MAP.FAILED,
                    STATUS_MAP.HEARTBEAT_TIMEOUT,
                    STATUS_MAP.QUEUE_TIMEOUT,
                    STATUS_MAP.EXEC_TIMEOUT
                ].includes(this.status)
            },
            isSucceed () {
                return [
                    STATUS_MAP.SUCCEED
                ].includes(this.status)
            },
            isPause () {
                return [
                    STATUS_MAP.PAUSE
                ].includes(this.status)
            },
            defaultStatus () {
                return [
                    STATUS_MAP.SKIP,
                    STATUS_MAP.QUALITY_CHECK_FAIL
                ].includes(this.status) || !this.status
            }
        }
    }
</script>

<style lang="scss">
@import "./conf";
.stage-status {
    position: relative;
    text-align: center;
    overflow: hidden;
    font-size: 14px;
    width: $serialSize;
    height: $serialSize;
    box-sizing: border-box;
    
    > span {
        position: absolute;
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        left: 0;
        top: 0;
        transition: all .3s cubic-bezier(1.0, 0.5, 0.8, 1.0);
    }

    &.matrix {
        width: $minSerialSize;
        height: $minSerialSize;
    }

    .status-logo {
        position: absolute;
        left: 15px;
        top: 15px;
    }
    
    .slide-top-enter, .slide-top-leave-to {
        transform: translateY($serialSize);
    }
    .slide-down-enter, .slide-down-leave-to {
        transform: translateY(-$serialSize);
    }
    .slide-left-enter, .slide-left-leave-to {
        transform: translateX($serialSize);
    }
    .slide-right-enter, .slide-right-leave-to {
        transform: translateX(-$serialSize);
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

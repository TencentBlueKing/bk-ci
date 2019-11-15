<template>
    <div class="progress-bar">
        <div class="progress-bar-background"></div>
        <div class="progress-bar-stripe"
            :class="status"
            :style="{
                width: percentage
            }">
            <i class="bk-icon icon-check-1 success status-icon" v-if="hasIcon && status === 'success'"></i>
            <i class="bk-icon icon-exclamation error status-icon" v-if="hasIcon && status === 'error' || status === 'known_error'"></i>
        </div>
    </div>
</template>

<script>
    export default {
        props: {
            percentage: {
                type: String
            },
            status: {
                type: String
            },
            hasIcon: {
                type: Boolean,
                default: false
            }
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf';

    .progress-bar {
        position: relative;
        width: 100%;
        height: 24px;
        overflow: hidden;
        border-radius: 12px;
        &-background {
            height: 100%;
            background-color: $borderColor;
        }
        &-stripe {
            position: absolute;
            top: 0;
            right: 0;
            bottom: 0;
            left: 0;
            height: 100%;
            border-radius: 12px;
            transition: width .3s linear;
            &.running,
            &.success {
                background: linear-gradient(to right, $primaryColor, #00b4ff);
            }
            &.warning, &.reviewing {
                background: linear-gradient(to right, $warningColor, #ffcc51);
            }
            &.error,
            &.known_error {
                background: linear-gradient(to right, $dangerColor, #ff7979);
            }
            .bk-icon {
                position: absolute;
                top: 50%;
                transform: translateY(-50%);
                color: #fff;
                font-size: 12px;
            }
            .status-icon {
                &.success {
                    right: 10px;
                }
                &.error,
                &.known_error {
                    left: 10px;
                }
            }
        }
    }
</style>

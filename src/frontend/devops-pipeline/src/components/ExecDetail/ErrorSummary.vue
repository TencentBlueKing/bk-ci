<template>
    <div class="error-log-summary">
        <p @click="toggleCollapse" class="error-log-summary-header">
            <span>
                <i :class="['devops-icon icon-angle-down', {
                    collapsed: isCollapse
                }]"></i>
                {{ errorTypeAlias }}
            </span>
            <span>{{ error.errorCode }}</span>
        </p>
        <div
            v-if="!isCollapse"
            class="error-log-summary-content"
            v-html="errorMsg"
        ></div>
    </div>
</template>

<script>
    import { errorTypeMap } from '@/utils/pipelineConst'

    export default {
        props: {
            error: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                isCollapse: false
            }
        },
        computed: {
            errorTypeAlias () {
                return this.$t(errorTypeMap[this.error?.errorType]?.title ?? '--')
            },
            errorMsg () {
                return this.error?.errorMsg?.trim().replace(/\n/g, '<br/>') ?? ''
            }
        },
        methods: {
            toggleCollapse () {
                this.isCollapse = !this.isCollapse
            }
        }
    }
</script>

<style lang="scss">
    .error-log-summary{
        position: relative;
        padding: 12px;
        display: flex;
        flex-direction: column;
        font-size: 12px;
        background: #404145;
        &-header {
            flex-shrink: 0;
            color: #f73131;
            display: flex;
            align-items: center;
            span:first-child {
                display: flex;
                align-items: center;
                &:after {
                    content: '-';
                    margin: 0 4px;
                    font-size: 12px;
                    color: #f73131;
                }
                > .devops-icon {
                    margin-right: 10px;
                    font-weight: bold;
                    font-size: 10px;
                    transform: rotate(0);
                    &.collapsed {
                        transition: transform 0.3s ease;
                        transform: rotate(-90deg);
                    }
                }
            }
        }
        &-content {
            padding: 8px 34px;
            margin: 0;
            position: absolute;
            color: white;
            top: 40px;
            left: 0;
            background: #404145;
            width: 100%;
            overflow: auto;
            z-index: 1;
            max-height: 360px;
            a {
                color: #3c96ff;
                &:visited {
                    color: #3c96ff;
                }
            }
        }
    }
</style>

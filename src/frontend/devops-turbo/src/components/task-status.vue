<template>
    <span class="status-component">
        <logo size="14" :name="lowerCaseProp()" :class="`${status} status-logo`"></logo><span :class="{ error: message }" v-bk-tooltips="{ content: message, disabled: !message }" v-if="showName">{{ getStatusName() }}</span>
    </span>
</template>

<script>
    import logo from './logo'

    export default {
        components: {
            logo
        },

        props: {
            status: String,
            message: String,
            showName: {
                type: Boolean,
                default: true
            }
        },

        methods: {
            lowerCaseProp () {
                return (this.status || '').toLowerCase()
            },

            getStatusName () {
                const statusMap = {
                    init: this.$t('turbo.初始化'),
                    staging: this.$t('turbo.申请资源中'),
                    starting: this.$t('turbo.启动worker中'),
                    running: this.$t('turbo.正在构建'),
                    finish: this.$t('turbo.构建完成'),
                    failed: this.$t('turbo.构建失败')
                }
                return statusMap[this.status]
            }
        }
    }
</script>

<style lang="scss" scoped>
    .status-component {
        display: flex;
        align-items: center;
        .status-logo {
            margin-right: 6px;
        }
        .failed {
            color: #fd9c9c;
        }
        .finish {
            color: #86e7a9;
        }
        .error {
            text-decoration: underline dotted #fd9c9c;
        }
    }
</style>

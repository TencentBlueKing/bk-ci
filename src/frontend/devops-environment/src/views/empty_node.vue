<template>
    <div class="empty-node-wrapper">
        <p class="title">{{ emptyInfo.title }}</p>
        <p class="intro-prompt">{{ emptyInfo.desc }}</p>
        <div class="create-node-row" v-if="isEnv">
            <bk-button theme="primary" class="create-env-btn" @click="toCreateNode">{{ $t('environment.create') }}</bk-button>
        </div>
        <div class="create-node-row" v-else>
            <bk-button theme="primary" class="import-node-btn" @click="toImportNode('construct')">{{ $t('environment.nodeInfo.importNode') }}</bk-button>
        </div>
    </div>
</template>

<script>
    export default {
        props: {
            isEnv: {
                type: Boolean,
                default: false
            },
            emptyInfo: Object,
            toCreateNode: Function,
            toImportNode: Function
        },
        data () {
            return {
                isDropdownShow: false
            }
        },
        methods: {
            dropdownShow () {
                this.isDropdownShow = true
            },
            dropdownHide () {
                this.isDropdownShow = false
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import './../scss/conf';

    .empty-node-wrapper {
        padding-top: 139px;
        text-align: center;

        .title {
            color: #333C48;
            font-size: 24px;
        }

        .intro-prompt {
            margin-top: 12px;
            font-size: 14px;
        }

        .create-node-row {
            margin-top: 28px;

            .create-node-btn {
                margin-right: 4px;
            }

            .create-env-btn {
                margin-left: 4px;
            }
        }
    }
</style>

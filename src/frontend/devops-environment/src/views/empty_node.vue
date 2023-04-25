<template>
    <div class="empty-node-wrapper">
        <p class="title">{{ emptyInfo.title }}</p>
        <p class="intro-prompt">{{ emptyInfo.desc }}</p>
        <div class="create-node-row" v-if="isEnv">
            <bk-button theme="primary" class="create-env-btn" @click="toCreateNode">{{ $t('environment.create') }}</bk-button>
        </div>
        <div class="create-node-row" v-else>
            <template v-if="isExtendTx">
                <bk-button theme="primary" @click="toImportNode('cmdb')">{{ $t('environment.nodeInfo.idcTestMachine') }}</bk-button>
                <bk-button theme="primary" @click="toImportNode('construct')">{{ $t('environment.thirdPartyBuildMachine') }}</bk-button>
            </template>
            <bk-button theme="primary" class="import-node-btn" v-else @click="toImportNode('construct')">{{ $t('environment.nodeInfo.importNode') }}</bk-button>
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
            display: flex;
            align-items: center;
            justify-content: center;

            .create-env-btn {
                margin-left: 4px;
            }
        }
    }
</style>

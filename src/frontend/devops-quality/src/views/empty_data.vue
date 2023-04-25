<template>
    <div class="empty-data-wrapper">
        <p class="title">{{emptyInfo.title}}</p>
        <p class="intro-prompt">{{emptyInfo.desc}}</p>
        <div class="create-node-row">
            <bk-button theme="primary" class="create-node-btn" @click="toCreateFn">{{$t('quality.新增')}}</bk-button>
        </div>
    </div>
</template>

<script>
    export default {
        props: {
            emptyInfo: Object,
            toCreateFn: Function
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/conf.scss';

    .empty-data-wrapper {
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

            .bk-button {
                width: 120px;
            }

            .create-node-btn {
                margin-right: 4px;
            }

            .create-env-btn {
                margin-left: 4px;
            }
        }
    }
</style>

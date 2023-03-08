<template>
    <article :class="{ 'metadata-list': true, 'left-render': isLeftRender }">
        <div class="title">{{ $t('metaData') }}</div>
        <div class="data-head">
            <div class="key-head">{{ $t('view.key')}}</div>
            <div class="value-head">{{ $t('view.value')}}</div>
        </div>
        <div class="data-row" v-for="(row, index) in metadataList" :key="index">
            <div class="key-item" :title="row.key">{{ row.key }}</div>
            <div class="value-item" :title="row.value">{{ row.value }}</div>
        </div>
        <div class="data-row empty-row" v-if="!metadataList.length">
            {{ $t('noData')}}
        </div>
    </article>
</template>

<script>
    export default {
        props: {
            path: {
                type: String,
                default: ''
            },
            isLeftRender: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                metadataList: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            path (newVal, oldVal) {
                if (newVal) this.requestList()
            }
        },
        methods: {
            async requestList () {
                const res = await this.$store.dispatch('pipelines/requestMetadataInfo', {
                    projectId: this.projectId,
                    artifactoryType: 'CUSTOM_DIR',
                    path: this.path
                })
                this.metadataList.splice(0, this.metadataList.length)
                Object.keys(res.meta).forEach(item => {
                    this.metadataList.push({
                        key: item,
                        value: res.meta[item]
                    })
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/conf';
    .metadata-list {
        position: absolute;
        top: -26px;
        left: 50px;
        width: 420px;
        min-width: 420px;
        height: min-content;
        border: 1px solid $borderWeightColor;
        border-radius: 2px;
        z-index: 99;
        cursor: default;
        &:before {
            content: '';
            position: absolute;
            width: 8px;
            height: 8px;
            font-size: 0;
            line-height: 0;
            overflow: hidden;
            border-width: 1px;
            border-style: solid solid solid solid;
            border-color: $borderWeightColor transparent transparent $borderWeightColor;
            background-color: #fff;
            top: 12px;
            left: -5px;
            transform: rotate(45deg);
            -webkit-transform: rotate(-45deg);
        }
        .title {
            padding-left: 16px;
            line-height: 42px;
            border-bottom: 1px solid $borderWeightColor;
            font-weight: bold;
            color: #333C48;
            background-color: $bgHoverColor;
        }
        .data-head,
        .data-row {
            display: flex;
            line-height: 42px;
            background-color: $bgHoverColor;
        }
        .data-head {
            color: #333C48;
        }
        .key-head,
        .value-head,
        .key-item,
        .value-item {
            flex: 2;
            padding-left: 16px;
            color: $fontColor;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .value-item {
            width: 200px;
            padding-right: 10px;
        }
        .key-head {
            border-right: 1px solid $borderWeightColor;
        }
        .data-row {
            background-color: #fff;
            border-top: 1px solid $borderWeightColor;
            font-size: 12px;
        }
        .empty-row {
            padding-left: 18px;
            color: $fontLighterColor;
        }
    }
    .left-render {
        left: unset;
        right: 50px;
        &:before {
            left: unset;
            right: -5px;
            transform: rotate(135deg);
            -webkit-transform: rotate(135deg);
        }
    }
</style>

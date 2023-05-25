<template>
    <div class="biz-container bkdevops-import-edit-subpage pipeline-subpages">
        <inner-header class="customer-inner-header">
            <div class="history-bread-crumb" slot="left">
                <bread-crumb class="bread-crumb-comp" separator="/">
                    <bread-crumb-item v-for="(crumb, index) in breadCrumbs" :key="index" v-bind="crumb">
                    </bread-crumb-item>
                </bread-crumb>
            </div>
            <template slot="right">
                <bk-button @click="savePipeline" :disabled="saveBtnDisabled" :icon="saveStatus ? 'loading' : ''" theme="primary">
                    {{ $t('save') }}
                </bk-button>
            </template>
        </inner-header>
        <router-view class="biz-content"></router-view>
    </div>
</template>

<script>
    import BreadCrumb from '@/components/BreadCrumb'
    import BreadCrumbItem from '@/components/BreadCrumb/BreadCrumbItem'
    import innerHeader from '@/components/devops/inner_header'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'import-pipeline-edit',
        components: {
            BreadCrumb,
            BreadCrumbItem,
            innerHeader
        },
        mixins: [pipelineOperateMixin],

        computed: {
            ...mapState('atom', [
                'importedPipelineJson',
                'saveStatus'
            ]),
            saveBtnDisabled () {
                return this.saveStatus || this.executeStatus || Object.keys(this.pipelineSetting).length === 0
            },
            breadCrumbs () {
                return [{
                    icon: 'pipeline',
                    selectedValue: this.$t('pipeline'),
                    to: {
                        name: 'PipelineManageList'
                    }
                }, ...(this.pipeline
                    ? [{
                        selectedValue: this.pipeline.name
                    }, {
                        selectedValue: this.$t('edit')
                    }]
                    : [])]
            }
        },
        created () {
            if (!this.importedPipelineJson) {
                this.$router.push({
                    name: 'PipelineManageList'
                })
            }
        },
        beforeDestroy () {
            this.setImportedPipelineJson(null)
            this.resetPipelineSetting()
            this.setPipeline(null)
            this.setPipelineEditing(false)
        },
        methods: {
            ...mapActions({
                setPipelineEditing: 'atom/setPipelineEditing',
                setPipeline: 'atom/setPipeline',
                setImportedPipelineJson: 'atom/setImportedPipelineJson',
                resetPipelineSetting: 'pipelines/resetPipelineSetting'
            }),
            async savePipeline () {
                const { data } = await this.save()
                const pipelineId = data.data
                this.$router.push({
                    name: 'pipelinesEdit',
                    params: {
                        pipelineId
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import "../../scss/conf";
    .bkdevops-import-edit-subpage {
        flex-direction: column;
        .pipelines-triggers.bkdevops-header-trigger-btn {
            width: auto;
            display: inline-block;
            height: auto;
            font-size: inherit;
        }
        .history-bread-crumb {
            display: flex;
            height: 100%;
            align-items: center;
            .bread-crumb-comp {
                flex: 1;
            }
        }
             .bkdevops-pipeline-tab-card {
        // display: flex;
        // overflow: hidden;
        // flex-direction: column;
        min-height: 100%;
        border: 0;
        background-color: transparent;
        &-setting {
            font-size: 18px;
            display: flex;
            align-items: center;
            height: 100%;

            .devops-icon {
                color: $fontLighterColor;
                padding-left: 16px;
                cursor: pointer;
                &:hover,
                &.active {
                    color: $primaryColor;
                }
            }
        }
        .bk-tab-header {
            background: transparent;
            background-image: none !important;
            .bk-tab-label-wrapper .bk-tab-label-list .bk-tab-label-item {
                min-width: auto;
                padding: 0;
                margin-right: 30px;
                text-align: left;
                font-weight: bold;
                &.active {
                    color: $primaryColor;
                    background: transparent
                }
            }
        }
        .bk-tab-section {
            width: 100%;
            min-height: calc(100% - 60px);
            padding: 0;
            margin-top: 10px;
            flex: 1;
            // overflow: hidden;
            .bk-tab-content {
                height: 100%;
                display: flex;
                flex-direction: column;
            }
        }
    }
    }
</style>

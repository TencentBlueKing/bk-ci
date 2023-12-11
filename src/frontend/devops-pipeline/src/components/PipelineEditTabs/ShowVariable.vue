<template>
    <section class="variable-version-wrapper">
        <div class="variable-entry" :class="{ 'is-close': !showVariable }" @click="toggleOpenVar">
            <i class="bk-icon icon-angle-double-right"></i>
            变量
        </div>
        <div v-if="showVariable" class="variable-version-container">
            <div class="select-tab-container">
                <div class="tab-content">
                    <div
                        v-for="(panel, index) in panels"
                        class="tab-item"
                        :key="index"
                        :class="{ 'actived': active === panel.name }"
                        @click="selectTab(panel.name)"
                    >
                        {{panel.label}}
                    </div>
                </div>
            </div>
            <div class="content-wrapper">
                <pipeline-param v-if="active === 'pipeline'" :editable="editable" :params="params" :update-container-params="handleContainerChange" />
                <atom-output-var :stages="stages" v-else-if="active === 'atomOutput'" />
                <system-var :container="container" v-else-if="active === 'system'" />
                <pipeline-version v-else :params="params" :disabled="!editable" :build-no="buildNo" :update-container-params="handleContainerChange" />
            </div>
        </div>
    </section>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import PipelineParam from './components/pipeline-param'
    import PipelineVersion from './components/pipeline-version'
    import AtomOutputVar from './components/atom-output-var'
    import SystemVar from './components/system-var'

    export default {
        components: {
            PipelineParam,
            PipelineVersion,
            AtomOutputVar,
            SystemVar
        },
        props: {
            pipeline: {
                type: Object,
                required: true
            },
            editable: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                panels: [
                    { name: 'pipeline', label: this.$t('流水线变量') },
                    { name: 'atomOutput', label: this.$t('插件输出变量') },
                    { name: 'system', label: this.$t('系统变量') },
                    { name: 'version', label: this.$t('推荐版本号') }
                ],
                active: 'pipeline'
            }
        },
        computed: {
            ...mapState('atom', [
                'showVariable'
            ]),
            stages () {
                return this.pipeline?.stages || []
            },
            container () {
                return this.pipeline?.stages[0]?.containers[0] || {}
            },
            params () {
                return this.container?.params || []
            },
            buildNo () {
                return this.container?.buildNo || {}
            }
        },
        created () {
            this.setShowVariable(true)
            this.requestCommonParams()
        },
        beforeDestroy () {
            this.setShowVariable(false)
        },
        methods: {
            ...mapActions('atom', [
                'setShowVariable',
                'toggleAtomSelectorPopup',
                'updateContainer',
                'requestCommonParams'
            ]),
            handleContainerChange (name, value) {
                this.updateContainer({
                    container: this.container,
                    newParam: {
                        [name]: value
                    }
                })
                console.log(this.params, 'final')
            },
            toggleOpenVar () {
                // 打开变量面板的同时关闭插件选择
                if (!this.showVariable) {
                    this.toggleAtomSelectorPopup(false)
                }
                this.setShowVariable(!this.showVariable)
            },
            selectTab (tab) {
                this.active = tab
            }

        }
    }
</script>

<style lang="scss">
    @import "@/scss/mixins/ellipsis.scss";
    @import "@/scss/mixins/scroller.scss";
    .variable-entry {
        z-index: 2017;
        position: absolute;
        right: 460px;
        top: calc(50% - 50px);
        writing-mode: vertical-lr;
        padding: 30px 2px;
        background: #A3C5FD;
        cursor: pointer;
        border-radius: 0 4px 4px 0;
        font-size: 12px;
        line-height: 16px;
        color: #FFF;
        .bk-icon {
            font-size: 14px;
        }
        &.is-close {
            right: 0;
            border-radius: 4px 0 0 4px;
            background: #699DF4;
            .bk-icon {
                display: inline-block;
                transform: rotate(180deg);
            }
        }
    }
    .variable-version-container {
        z-index: 2016;
        width: 480px;
        position: absolute;
        top: 0;
        right: 0;
        height: 100%;
        background-color: #FAFBFD;
        border: 1px solid #DCDEE5;
        .select-tab-container {
            border-bottom: 1px solid #ebf0f5;
            .tab-content {
                height: 40px;
                padding: 0 24px;
                display: flex;
                align-items: center;
                .tab-item {
                    margin-right: 40px;
                    line-height: 40px;
                    cursor: pointer;
                    font-size: 14px;
                    color: #63656E;
                    &.actived {
                        color: #3A84FF;
                        border-bottom: 2px solid #3A84FF;
                    }
                    &:last-child {
                        margin-right: 16px;
                    }
                }
            }
        }
        .content-wrapper {
            height: calc(100% - 40px);
            padding: 8px 24px 20px;
            overflow-y: auto;
            @include scroller(#a5a5a5, 4px);
        }
    }
    .current-edit-param-item {
        z-index: 11;
        width: 480px;
        position: absolute;
        top: 0;
        right: 0;
        height: 100%;
        background-color: #FFF;
        border-left: 1px solid #DCDEE5;
        .edit-var-header {
            display: flex;
            align-items: center;
            height: 40px;
            font-size: 16px;
            color: #313238;
            border-bottom: 1px solid #DCDEE5;
            padding: 0 4px;
            .back-icon {
                cursor: pointer;
                font-size: 28px;
                color: #3A84FF;
            }
        }
        .edit-var-content {
            height: calc(100% - 90px);
            overflow-y: auto;
            padding: 24px 40px;
        }
        .edit-var-footer {
            position: absolute;
            bottom: 0;
            width: 100%;
            display: flex;
            align-items: center;
            height: 48px;
            padding: 0 24px;
            border-top: 1px solid #DCDEE5;
        }
    }
</style>

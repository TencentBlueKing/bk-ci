<template>
    <section class="variable-version-wrapper">
        <div class="variable-entry" :class="{ 'is-close': !openVar }" @click="toggleOpenVar">
            <i class="bk-devops-icon bk-icon icon-expand-line"></i>
            变量
        </div>
        <div v-if="openVar" class="variable-version-container">
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
                <pipeline-param v-if="active === 'variable'" :params="params" :update-container-params="handleContainerChange" />
                <div v-else>
                    <pipeline-version :params="params" :build-no="buildNo" :update-container-params="handleContainerChange" />
                </div>
            </div>
        </div>
    </section>
</template>

<script>
    import { mapActions } from 'vuex'
    import PipelineParam from './components/pipeline-param'
    import PipelineVersion from './components/pipeline-version'

    export default {
        components: {
            PipelineParam,
            PipelineVersion
        },
        props: {
            pipeline: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                panels: [
                    { name: 'variable', label: '流水线变量' },
                    { name: 'version', label: '推荐版本号' }
                ],
                active: 'variable',
                openVar: true
            }
        },
        computed: {
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
        methods: {
            ...mapActions('atom', [
                'updateContainer'
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
                this.openVar = !this.openVar
            },
            selectTab (tab) {
                this.active = tab
            }
            
        }
    }
</script>

<style lang="scss">
    .edit-var-container {
        .edit-var-content {
            padding: 20px 24px;
        }
        .bk-sideslider-footer {
            position: absolute;
            bottom: 0;
            .edit-var-footer {
                margin-left: 24px;
            }
        }
        /* .bk-s */
    }
    .variable-entry {
        z-index: 100;
        position: absolute;
        right: 480px;
        top: 24px;
        writing-mode: vertical-lr;
        padding: 8px 4px;
        background: #C4C6CC;
        cursor: pointer;
        border-radius: 4px 0 0 4px;
        font-size: 12px;
        line-height: 16px;
        color: #FFF;
        &.is-close {
            right: 0px;
            .icon-expand-line {
                display: inline-block;
                transform: rotate(180deg);
            }
        }
    }
    .variable-version-container {
        z-index: 10;
        width: 480px;
        position: absolute;
        top: 0;
        right: 0;
        height: 100%;
        background-color: #FAFBFD;
        padding: 8px 0px 16px;
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
                }
            }
        }
        .content-wrapper {
            padding: 8px 24px 20px;
            .variable-container {
                .circle {
                    width: 10px;
                    height: 10px;
                    border-radius: 50%;
                }
                .add-and-desc {
                    margin: 16px 0;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    .status-desc {
                        display: flex;
                        align-items: center;
                        .desc-item {
                            display: flex;
                            align-items: center;
                            margin-left: 16px;
                            font-size: 12px;
                            color: #979BA5;
                            .status-desc {
                                margin-left: 4px;
                            }
                        }
                    }
                }
                .variable-content {
                    width: 100%;
                    min-height: 64px;
                    border: 1px solid #DCDEE5;
                    border-bottom: none;
                    .variable-empty {
                        height: 200px;
                        border-bottom: 1px solid #DCDEE5;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-size: 14px;
                        color:#63656E;
                    }
                    .variable-item {
                        position: relative;
                        height: 64px;
                        border-bottom: 1px solid #DCDEE5;
                        padding-left: 24px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        .var-con {
                            font-size: 12px;
                            letter-spacing: 0;
                            line-height: 20px;
                            .var-names {
                                color: #313238;
                            }
                            .default-value {
                                color: #979BA5;
                            }
                        }
                        .var-operate {
                            .var-status {
                                margin-right: 16px;
                                display: flex;
                                align-items: center;
                                .circle {
                                    margin-left: 8px;
                                }
                            }
                            .operate-btns {
                                width: 76px;
                                height: 62px;
                                background-color: #F5F7FA;
                                display: flex;
                                align-items: center;
                                padding: 0 18px;
                                i {
                                    cursor: pointer;
                                    font-size: 14px;
                                    color: #63656E;
                                }
                            }
                        }
                    }
                }
            }
            .version-container {

            }
        }
    }
</style>

<template>
    <bk-dialog
        v-model="isShow"
        width="1080"
        :position="dialogPositionConfig"
        class="variable-group-preview-dialog"
        @after-leave="handleHideShow"
    >
        <template slot="header">
            <div class="preview-header">
                {{ $t('publicVar.varGroupPreview') }}
                <span class="group-name">
                    | {{ groupData.groupName }}
                </span>
            </div>
        </template>
        <div class="preview-content-main">
            <div class="left-wrapper">
                <div class="tab-item active">
                    <div>
                        <span class="operate-type">修改变量</span>
                        <span class="var-name">
                            MAX_COUNT
                        </span>
                    </div>
                </div>
                <div class="tab-item">
                    <div>
                        <span class="operate-type">修改变量</span>
                        <span class="var-name">
                            MAX_COUNT
                        </span>
                    </div>
                </div>
                <div class="tab-item">
                    <div>
                        <span class="operate-type">修改变量</span>
                        <span class="var-name">
                            MAX_COUNT
                        </span>
                    </div>
                </div>
            </div>
            <div class="right-wrapper">
                <div class="title">
                    {{ $t('publicVar.modifiedTitle') }}
                </div>
                <div class="change-content">
                    <span class="key">常量别名 </span>
                    <span class="value">
                        <span class="current-value">aaa</span>
                        <Logo
                            class="arrow-right-icon"
                            size="14"
                            name="arrow-right"
                        />
                        <span class="after-value">bbb</span>
                    </span>
                </div>
                <div class="references-list">
                    <div class="title">
                        <div>
                            {{ $t('publicVar.referencesTitle') }}
                            <span class="references-tips">
                                以下流水线/模板都将同步更新此常量属性
                            </span>
                        </div>
                    </div>
                    <render-related-table />
                    <render-related-table />
                </div>
            </div>
        </div>
        <footer slot="footer">
            <bk-button
                theme="primary"
                @click="handleRelease"
            >
                {{ $t('confirm') }}
            </bk-button>
        </footer>
    </bk-dialog>
</template>

<script setup>
    import { ref } from 'vue'
    import Logo from '@/components/Logo'
    import RenderRelatedTable from './RenderRelatedTable'
    import UseInstance from '@/hook/useInstance'
    const { proxy } = UseInstance()
    const props = defineProps({
        isShow: Boolean,
        groupData: Object
    })
    const dialogPositionConfig = ref({
        top: window.innerHeight < 800 ? '20' :  window.innerHeight > 1000 ? '8%' : '80'
    })
    function handleHideShow () {
        proxy.$emit('update:isShow', false)
    }
    function handleRelease () {
        proxy.$emit('update:isShow', false)
        proxy?.$router?.push({
            name: 'VariableRelease'
        })
    }
</script>

<style lang="scss">
    .variable-group-preview-dialog {
        .bk-dialog-body {
            overflow: auto;
        }
        .preview-header {
            color: #313238;
            text-align: left;
            margin-bottom: 10px;
        }
        .group-name {
            color: #979BA5;
        }
        .preview-content-main {
            display: flex;
            height: 600px;
        }
        .left-wrapper {
            display: flex;
            flex-direction: column;
            border-right: 1px solid #DCDEE5;
            width: 250px;
            .tab-item {
                position: relative;
                padding: 12px 28px;
                font-size: 14px;
                height: 45px;
                cursor: pointer;
                background-color: #F5F7FA;
                margin-bottom: 8px;
                &.active {
                    background-color: #fff;
                    border: 1px solid #DCDEE5;
                    border-right: none;
                    border-radius: 5px 0 0 5px;
                    &::after {
                        position: absolute;
                        top: 0;
                        right: -1px;
                        width: 1px;
                        height: 100%;
                        background: #FFF;
                        content: '';
                        z-index: 1000;
                    }
                }
                .operate-type {
                    color: #313238;
                }
                .var-name {
                    font-size: 12px;
                    padding-left: 16px;
                    color: #979BA5;
                }
            }
        }
        .right-wrapper {
            flex-grow: 1;
            padding: 14px 24px;
            border: 1px solid #DCDEE5;
            border-left: none;
            border-radius: 0 5px 5px 0;
            background-color: #fff;
            overflow: auto;
            .title {
                display: flex;
                align-items: center;
                justify-content: space-between;
                font-size: 14px;
                color: #4D4F56;
                font-weight: bold;
                margin-bottom: 8px;
            }
            .references-tips {
                color: #979BA5;
                font-size: 12px;
                margin-left: 16px;
                font-weight: 400;
            }
            .change-content {
                font-size: 12px;
                margin-bottom: 20px;
                .key {
                    background: #F0F1F5;
                    border-radius: 2px;
                    padding: 4px 8px;
                    margin-right: 8px;
                }
                .value {
                    display: inline-flex;
                    align-items: center;
                }
                .current-value {
                    color: #C4C6CC;
                }
                .after-value {
                    color: #4D4F56;
                }
                .arrow-right-icon {
                    color: #F8B64F;
                }
            }
        }
    }
</style>
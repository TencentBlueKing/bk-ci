<template>
    <bk-sideslider
        :is-show.sync="isShow"
        :width="sidesliderWidth"
        :show-mask="false"
        :transfer="false"
        ext-cls="param-group-sideslider"
        @hidden="handleHidden"
        :before-close="handleBeforeClose"
    >
        <template v-if="title">
            <div slot="header">
                {{ title }}
                <span
                    v-if="groupData.groupId"
                    class="group-name"
                >
                    | {{ groupData.groupId }}
                </span>
            </div>
            <div
                class="sideslider-content"
                slot="content"
            >
                <div class="header-wrapper">
                    <mode-switch
                        class="mode-switch"
                        read-only
                    />
                </div>
                <div class="content-wrapper">
                    <section class="left-box">
                        <p class="basic-info-title">
                            {{ $t('publicVar.basicInfo') }}
                        </p>
                        <bk-form
                            :label-width="200"
                            form-type="vertical"
                        >
                            <bk-form-item
                                :label="$t('publicVar.paramGroupId')"
                                :required="true"
                            >
                                <bk-input
                                    v-model="groupData.groupId"
                                />
                            </bk-form-item>
                            <bk-form-item
                                :label="$t('publicVar.paramGroupDesc')"
                            >
                                <bk-input
                                    type="textarea"
                                    v-model="groupData.desc"
                                />
                            </bk-form-item>
                        </bk-form>
                    </section>
                    <section class="right-box">
                        <div class="right-aside-header-wrapper">
                            <bk-button
                                icon="plus"
                                class="mr10"
                            >
                                {{ $t('publicVar.addParamGroup') }}
                            </bk-button>
                            <bk-button
                                icon="plus"
                                class="mr10"
                            >
                                {{ $t('publicVar.addParamGroup') }}
                            </bk-button>
                        </div>
                        <div>
                            todo
                        </div>
                    </section>
                </div>
            </div>
            <div
                class="sideslider-footer"
                slot="footer"
            >
                <bk-button
                    :disabled="releaseDisabled"
                    theme="primary"
                >
                    {{ $t('publicVar.release') }}
                </bk-button>
                <bk-button>
                    {{ $t('cancel') }}
                </bk-button>
            </div>
        </template>
        <template v-else>
            <div slot="header">
                <bk-tab
                    :active.sync="viewTab"
                    ext-cls="view-variable-tabs"
                    type="unborder-card"
                >
                    <bk-tab-panel
                        v-for="(panel, index) in panels"
                        v-bind="panel"
                        :key="index"
                    >
                    </bk-tab-panel>
                </bk-tab>
            </div>
        </template>
    </bk-sideslider>
</template>

<script setup>
    import { computed, watch, ref } from 'vue'
    import UseInstance from '@/hook/useInstance'
    import ModeSwitch from '@/components/ModeSwitch'

    const { proxy } = UseInstance()
    const props = defineProps({
        isShow: Boolean,
        title: String,
        groupData: Object
    })
    const viewTab = ref('basicInfo')
    const panels = ref([
        {
            name: 'basicInfo',
            label: proxy.$t('publicVar.basicInfo')
        },
        {
            name: 'referenceList',
            label: proxy.$t('publicVar.referenceList')
        },
        {
            name: 'releaseRecord',
            label: proxy.$t('publicVar.releaseRecord')
        }
    ])
    const releaseDisabled = computed(() => {
        return !props.groupData?.groupId
    })
    const sidesliderWidth = computed(() => {
        // 250 表格第一列宽度
        // 100 表格第二列宽度
        // 20 padding样式
        return window.innerWidth - 250 - 120 - 20
    })

    watch(() => props.groupData.groupId, () => {
        console.log(1)
    })
    function handleHidden () {
        proxy.$emit('update:isShow', false)
    }
    function handleBeforeClose () {
        return true
    }
</script>

<style lang="scss">
    .param-group-sideslider {
        top: 155px !important;
        .bk-sideslider-content {
            max-height: none !important;
            height: calc(100% - 100px) !important;
        }
        .sideslider-content {
            height: calc(100% - 65px);
        }
        .sideslider-footer {
            padding: 0 20px;
        }
        .group-name {
            color: #979BA5;
        }
        .header-wrapper {
            display: flex;
            align-items: center;
            padding: 0 20px;
            height: 65px;
            background: #FAFBFD;
            .mode-switch {
                width: 160px;
            }
        }
        .content-wrapper {
            display: flex;
            height: 100%;
            padding: 8px 20px 20px;
            background: #fff;
            .left-box {
                width: 260px;
                height: 100%;
                padding-right: 15px;
                border-right: 1px solid #DCDEE5;
            }
            .right-box {
                background: #fff;
                flex: 1;
                padding: 0 20px;
            }
            .right-aside-header-wrapper {
                margin-bottom: 16px;
            }
        }
        .basic-info-title {
            font-weight: 700;
            font-size: 14px;
            color: #63656E;
            margin-bottom: 10px;
        }
    }
    .view-variable-tabs {
        height: 52px !important;
        .bk-tab-header {
            height: 52px !important;
            background-image: none !important;
        }
    }
</style>

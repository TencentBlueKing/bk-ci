<template>
    <div class="net-policy-component">
        <!-- 上行带宽峰值 -->
        <bk-form-item
            :label="$t('store.上行带宽峰值')"
            property="maxPeakBandwidth"
            :required="true"
        >
            <div class="bandwidth-control">
                <div class="bandwidth-input-wrapper">
                    <bk-input
                        v-model.number="netPolicyInfo.maxPeakBandwidth"
                        type="number"
                        :min="0"
                        :show-controls="false"
                        class="bandwidth-input"
                        @input="val => updateField('maxPeakBandwidth', Number(val))"
                    >
                        <template #prepend>
                            <span
                                :class="['control-btn', netPolicyInfo.maxPeakBandwidth <= 0 ? 'disabled' : '']"
                                @click="decreaseBandwidth('max')"
                            >
                                <i class="bk-icon icon-minus" />
                            </span>
                        </template>
                        <template #append>
                            <span
                                class="control-btn text-14"
                                @click="increaseBandwidth('max')"
                            >
                                <i class="bk-icon icon-plus" />
                            </span>
                        </template>
                    </bk-input>
                    <span class="bandwidth-unit">Mbps</span>
                </div>
            </div>
        </bk-form-item>
        
        <!-- 下行带宽峰值 -->
        <bk-form-item
            :label="$t('store.下行带宽峰值')"
            property="minPeakBandwidth"
            :required="true"
        >
            <div class="bandwidth-control">
                <div class="bandwidth-input-wrapper">
                    <bk-input
                        v-model.number="netPolicyInfo.minPeakBandwidth"
                        type="number"
                        :min="0"
                        :show-controls="false"
                        class="bandwidth-input"
                        @input="val => updateField('minPeakBandwidth', Number(val))"
                    >
                        <template #prepend>
                            <span
                                :class="['control-btn', netPolicyInfo.minPeakBandwidth <= 0 ? 'disabled' : '']"
                                @click="decreaseBandwidth('min')"
                            >
                                <i class="bk-icon icon-minus" />
                            </span>
                        </template>
                        <template #append>
                            <span
                                class="control-btn text-14"
                                @click="increaseBandwidth('min')"
                            >
                                <i class="bk-icon icon-plus" />
                            </span>
                        </template>
                    </bk-input>
                    <span class="bandwidth-unit">Mbps</span>
                </div>
            </div>
        </bk-form-item>
        
        <!-- 需要访问的站点 -->
        <bk-form-item
            :label="$t('store.需要访问的站点')"
            property="needVisitedSiteInfos"
        >
            <site-info-table
                :site-infos="netPolicyInfo.needVisitedSiteInfos"
                :editable="true"
                class="mt10"
                @update:siteInfos="handleUpdateSiteInfos"
            />
        </bk-form-item>
    </div>
</template>

<script setup name="NetPolicy">
    import UseInstance from '@/hook/useInstance.js'
    import SiteInfoTable from '@/components/siteInfoTable.vue'

    const props = defineProps({
        netPolicyInfo: {
            type: Object,
            default: () => ({})
        }
    })

    const emit = defineEmits(['update:netPolicyInfo'])

    const { proxy } = UseInstance()
    const { $t } = proxy

    // 更新字段的辅助函数
    const updateField = (field, value) => {
        emit('update:netPolicyInfo', {
            ...props.netPolicyInfo,
            [field]: value
        })
    }

    function increaseBandwidth (type) {
        if (type === 'max') {
            updateField('maxPeakBandwidth', Math.max(0, props.netPolicyInfo.maxPeakBandwidth + 1))
        } else {
            updateField('minPeakBandwidth', Math.max(0, props.netPolicyInfo.minPeakBandwidth + 1))
        }
    }

    function decreaseBandwidth (type) {
        if (type === 'max') {
            updateField('maxPeakBandwidth', Math.max(0, props.netPolicyInfo.maxPeakBandwidth - 1))
        } else {
            updateField('minPeakBandwidth', Math.max(0, props.netPolicyInfo.minPeakBandwidth - 1))
        }
    }

    function handleUpdateSiteInfos (newSiteInfos) {
        updateField('needVisitedSiteInfos', newSiteInfos)
    }
</script>

<style lang="scss" scoped>
.net-policy-component {
    margin-bottom: 22px;
    .bandwidth-control {
        .bandwidth-input-wrapper {
            display: flex;
            align-items: center;
            width: 150px;
        }
        
        .bandwidth-input {
            width: 120px;
            border-radius: 2px 0 0 2px;
            
            ::v-deep .bk-input--number-control {
                display: none;
            }
        }
        
        .bandwidth-unit {
            display: inline-flex;
            align-items: center;
            height: 32px;
            padding: 0 8px;
            font-size: 12px;
            background: #fafbfd;
            border: 1px solid #c4c6cc;
            border-left: 0;
            border-radius: 0 2px 2px 0;
        }
        
        .control-btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 24px;
            height: 32px;
            font-size: 12px;
            cursor: pointer;
            font-weight: bold;
            
            &.disabled {
                opacity: 0.3;
                cursor: not-allowed;
            }
            
            &:not(.disabled):hover {
                color: #3a84ff;
            }
        }
    }
    
    .mt10 {
        margin-top: 10px;
    }
}
</style>

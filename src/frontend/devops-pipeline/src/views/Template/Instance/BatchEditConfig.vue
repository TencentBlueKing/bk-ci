<template>
    <bk-sideslider
        :is-show.sync="value"
        :width="1000"
        :title="$t('template.batchEditParams')"
        @shown="showBatchEditSlider"
        @hidden="hideBatchEditSlider"
        ext-cls="batch-edit-side-slider"
    >
        <template slot="content">
            <InstanceAddField
                :instance-list="instanceList"
                @confirm="confirmChange"
                @cancel="hideBatchEditSlider"
            />
        </template>
    </bk-sideslider>
</template>

<script setup>
    import { defineProps } from 'vue'
    import InstanceAddField from '@/components/Template/InstanceAddField'
    import UseInstance from '@/hook/useInstance'
    defineProps({
        value: Boolean,
        instanceList: Array
    })
    const { proxy } = UseInstance()
    function hideBatchEditSlider () {
        proxy.$emit('input', false)
    }
    function showBatchEditSlider () {
        proxy.$emit('input', true)
    }
    function confirmChange (params) {
        proxy.$emit('input', false)
        proxy.$emit('change', params)
    }
</script>

<style lang="scss">
    .batch-edit-content {
        padding: 20px;
    }
</style>

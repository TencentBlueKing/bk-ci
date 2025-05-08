<template>
    <bk-sideslider
        :is-show.sync="value"
        :width="640"
        :title="$t('template.batchEditInstance')"
        @shown="showBatchEditSlider"
        @hidden="hideBatchEditSlider"
        ext-cls="batch-edit-side-slider"
    >
        <template
            slot="content"
        >
            <div
                class="batch-edit-content"
            >
                <bk-alert
                    class="mb10"
                    type="warning"
                    :title="$t('template.batchEditInstanceTips')"
                />
                <InstanceAddField />
            </div>
        </template>
    </bk-sideslider>
</template>

<script setup>
    import { defineProps } from 'vue'
    import InstanceAddField from '@/components/Template/InstanceAddField'
    import UseInstance from '@/hook/useInstance'
    defineProps({
        value: Boolean
    })
    const { proxy } = UseInstance()
    function hideBatchEditSlider () {
        proxy.$emit('input', false)
    }
    function showBatchEditSlider () {
        proxy.$emit('input', true)
    }
</script>

<style lang="scss">
    .batch-edit-content {
        padding: 20px;
    }
</style>

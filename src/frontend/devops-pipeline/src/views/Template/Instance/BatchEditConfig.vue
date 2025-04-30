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
                <InstanceAddField />
            </div>
        </template>
    </bk-sideslider>
</template>

<script setup>
    import { defineProps } from 'vue'
    import InstanceAddField from '@/components/Template/InstanceAddField'
    defineProps({
        value: Boolean
    })
</script>

<style lang="scss">
    .batch-edit-content {
        padding: 20px;
    }
</style>

<template>
    <bk-dialog
        v-model="isShow"
        theme="primary"
        :width="600"
        :height="400"
        :title="title || $t('publicVar.importParamGroup')"
        :mask-close="false"
        :show-footer="false"
        @cancel="handleCancel"
    >
        <bk-upload
            v-if="isShow"
            accept=".yaml, .yml, application/x-yaml"
            :with-credentials="true"
            :custom-request="handleSelect"
        >
        </bk-upload>
        <i18n
            class="import-tips"
            tag="div"
            path="publicVar.importParamGroupLabel"
        >
            <span class="text-area">{{ $t('publicVar.templateFile') }}</span>
        </i18n>
    </bk-dialog>
</template>

<script setup>
    // import { ref, computed, onMounted, watch } from 'vue'
    import UseInstance from '@/hook/useInstance'
    const { proxy } = UseInstance()
    defineProps({
        isShow: Boolean,
        title: String,
        handleImportSuccess: Function
    })
    function handleSelect () {

    }
    
    function handleCancel () {
        proxy.$emit('update:isShow', false)
    }

</script>

<style lang="scss" scoped>
    .import-tips {
        font-size: 12px;
        margin-top: 10px;
        .text-area {
            color: #3a84ff;
            cursor: pointer;
        }
    }
</style>

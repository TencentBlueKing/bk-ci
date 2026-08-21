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
            :with-credentials="true"
            :custom-request="handleSelect"
        >
        </bk-upload>
        <p
            class="import-tips"
        >
            {{ $t('publicVar.importParamGroupLabel') }}
        </p>
    </bk-dialog>
</template>

<script setup>
    // import { ref, computed, onMounted, watch } from 'vue'
    import UseInstance from '@/hook/useInstance'
    const { proxy } = UseInstance()
    const props = defineProps({
        isShow: Boolean,
        title: String,
        successFn: Function
    })
    function handleSelect ({ fileObj, onProgress, onSuccess, onDone }) {
        const fileName = fileObj.name || ''
        const fileExtension = fileName.split('.').pop()?.toLowerCase()
        // 校验文件类型是否为 yaml
        if (!['yaml', 'yml'].includes(fileExtension)) {
            proxy.$bkMessage({
                theme: 'error',
                message: proxy.$t('publicVar.importFileTypeError')
            })
            onSuccess({
                code: 1,
                result: ''
            }, fileObj)
            onDone(fileObj)
            return
        }

        const reader = new FileReader()
        reader.readAsText(fileObj.origin)
        reader.addEventListener('loadend', async e => {
            try {
                const yaml = e.target.result
                const isValid = !!yaml
                const code = isValid ? 0 : 1
        
                onSuccess({
                    code,
                    result: yaml
                }, fileObj)
        
                if (isValid) {
                    props.successFn(yaml)
                }
           
            } catch (e) {
                onSuccess({
                    code: 1,
                    result: ''
                }, fileObj)
            } finally {
                onDone(fileObj)
            }
        })
        reader.addEventListener('progress', onProgress)
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

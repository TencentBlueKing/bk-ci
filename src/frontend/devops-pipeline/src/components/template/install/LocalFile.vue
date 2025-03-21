<template>
    <div class="file-upload">
        <bk-upload
            accept=".json, .yaml, .yml, application/json, application/x-yaml"
            :with-credentials="true"
            :custom-request="handleSelect"
        />
        <p class="tips">{{ $t('template.uploadTemplateYamlTips') }}</p>
    </div>
</template>

<script setup name='localFile'>
    // import { ref, computed } from 'vue'
    // import UseInstance from '@/hook/useInstance'
    // const { proxy } = UseInstance()
    // const projectId = computed(() => proxy.$route.params.projectId)
    // const repository = ref('')
    function handleSelect () {

    }
</script>

<style lang="scss">
    .file-upload {
        margin: 0 auto;
        width: 650px;
        height: 200px;
        margin-top: 30px;
        margin-bottom: 20px;
       
        .tips {
            margin-top: 10px;
        }
    }
</style>

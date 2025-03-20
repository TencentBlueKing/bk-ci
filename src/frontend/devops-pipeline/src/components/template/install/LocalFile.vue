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

<script>
    import { defineComponent, ref, computed } from '@vue/composition-api'
    import UseInstance from '@/hook/useInstance'
    export default defineComponent({
        name: 'localFile',
        setup (props, { root, emit }) {
            if (!root) return
            const { proxy } = UseInstance()
            const projectId = computed(() => proxy.$route.params.projectId)
            const repository = ref('')
            function handleSelect () {

            }
            return {
                projectId,
                repository,
                handleSelect
            }
        }
    })

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

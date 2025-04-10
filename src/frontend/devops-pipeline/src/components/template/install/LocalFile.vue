<template>
    <div class="file-upload">
        <bk-upload
            accept=".yaml, .yml, application/x-yaml"
            :with-credentials="true"
            :custom-request="handleSelect"
        />
        <p class="tips">{{ $t('template.uploadTemplateYamlTips') }}</p>
    </div>
</template>

<script setup name='localFile'>
    import { nextTick, computed, defineProps, defineExpose } from 'vue'
    import UseInstance from '@/hook/useInstance'
    import { CODE_MODE } from '@/utils/pipelineConst'
    const { proxy, i18n, showTips } = UseInstance()
    const routeProjectId = computed(() => proxy.$route.params.projectId)
    const props = defineProps([
        {
            handleImportSuccess: Function,
            pipelineId: String
        }
    ])
    defineExpose({ handleSelect })
    function handleSelect ({ fileObj, onProgress, onSuccess, onDone }) {
        const reader = new FileReader()
        reader.readAsText(fileObj.origin)
        reader.addEventListener('loadend', async e => {
            try {
                if (fileObj.type === 'application/x-yaml' || fileObj.name.endsWith('.yaml') || fileObj.name.endsWith('.yml')) {
                    const yaml = e.target.result
                    
                    const isValid = !!yaml
                    const code = isValid ? 0 : 1
                    const message = isValid ? null : i18n.t('invalidPipelineJsonOrYaml')

                    onSuccess({
                        code,
                        message,
                        result: yaml
                    }, fileObj)

                    if (isValid) {
                        handleSuccess(yaml)
                    }
                }
            } catch (e) {
                console.log(e)
                onSuccess({
                    code: 1,
                    message: i18n.t('invalidPipelineJsonOrYaml'),
                    result: ''
                }, fileObj)
            } finally {
                onDone(fileObj)
            }
        })
        reader.addEventListener('progress', onProgress)
    }
    async function handleSuccess (result) {
        proxy.$store.dispatch('updatePipelineMode', CODE_MODE)
        const res = await updateCodeModePipeline(result)

        if (res) {
            if (typeof props.handleImportSuccess === 'function') {
                props.handleImportSuccess()
                return
            }
            nextTick(() => {
                proxy.$router.push({
                    name: 'pipelineImportEdit',
                    params: {
                        tab: 'pipeline',
                        isTemplatePipeline: true
                    }
                })
            })
        }
    }
    async function updateCodeModePipeline (result) {
        proxy.$store.dispatch('atom/setPipelineYaml', result)
        proxy.$store.dispatch('atom/setEditFrom', true)
        try {
            const { templateModel, templateSetting, templateType } = await proxy.$store.dispatch('atom/transferTemplatePipeline', {
                projectId: routeProjectId.value,
                storageType: 'YAML',
                yaml: result
            })
            const newPipelineName = templateModel.name
                    
            const pipeline = {
                ...templateModel,
                name: newPipelineName
            }
            proxy.$store.dispatch('atom/setPipelineSetting', {
                ...templateSetting.setting,
                pipelineName: newPipelineName
            })
            proxy.$store.dispatch('atom/setPipeline', pipeline)
            proxy.$store.dispatch('atom/setTemplateType', templateType)
            proxy.$store.dispatch('atom/setPipelineWithoutTrigger', {
                ...pipeline,
                stages: templateModel.stages.slice(1)
            })
            proxy.$store.dispatch('atom/setPipelineEditing', true)

            return true
        } catch (error) {
            showTips({
                message: error.message,
                theme: 'error'
            })
            return false
        }
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

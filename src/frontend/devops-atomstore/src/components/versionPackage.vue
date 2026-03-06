<template>
    <div class="version-package-component">
        <bk-upload
            ref="uploadRef"
            :with-credentials="true"
            :url="uploadUrl"
            :name="'file'"
            :accept="'.zip,.tar,.exe,.msi'"
            :files="files"
            :limit="1"
            :size="5000"
            :multiple="false"
            :handle-res-code="handleResCode"
            @on-exceed="handleExceed"
            @on-delete="handleDelete"
            @on-error="handleError"
            @on-progress="handleProgress"
            @on-success="handleSuccess"
        >
        </bk-upload>
    </div>
</template>

<script setup name="VersionPackage">
    import { ref, computed } from 'vue'
    import UseInstance from '@/hook/useInstance.js'
    import { BASE_PREFIX, STORE_TYPE } from '@/utils/constants'

    const props = defineProps({
        storeCode: {
            type: String,
            required: true
        },
        storeId: {
            type: String,
            required: true
        },
        version: {
            type: String,
            required: true
        },
        lastVersion: {
            type: String,
            required: true
        },
        releaseType: {
            type: String,
            required: true
        }
    })

    const emit = defineEmits(['uploaded', 'delete', 'progress'])

    const { proxy } = UseInstance()
    const { $bkMessage, $t } = proxy

    const files = ref([])

    const uploadUrl = computed(() => {
        const query = `version=${props.version}&releaseType=${props.releaseType}`
        return `${BASE_PREFIX}/artifactory/api/user/artifactories/store/component/types/${STORE_TYPE}/ids/${props.storeId}/codes/${props.storeCode}/pkg/archive?${query}`
    })

    function handleResCode (res) {
        emit('uploaded', res.data)
        if (res.message) {
            $bkMessage({
                theme: 'error',
                message: res.message
            })
        }
        return res.data
    }

    function handleDelete () {
        files.value = []
        emit('delete')
    }

    function handleError (_file, fileList, error) {
        files.value = []
        emit('delete')
        $bkMessage({
            theme: 'error',
            message: error
        })
    }

    function handleExceed () {
        $bkMessage({
            message: $t('store.文件超出最大上传数'),
            theme: 'error'
        })
    }

    function handleProgress (event, _file, fileList) {
        emit('progress', true)
    }

    function handleSuccess (_file, fileList) {
        emit('progress', false)
    }

    function handleBeforeUpload () {
        if (props.releaseType !== 'CANCEL_RE_RELEASE' && props.lastVersion === props.version) {
            $bkMessage({
                theme: 'error',
                message: $t('store.版本号已存在')
            })
            return false
        }
        return true
    }

    function empty () {
        files.value = []
    }

    defineExpose({
        empty
    })
</script>

<style lang="scss" scoped>
.version-package-component {
    .upload-file-item {
        display: flex;
        align-items: center;
        padding: 10px;
        width: 100%;
        
        .bk-icon {
            font-size: 32px;
            color: #979ba5;
            margin-right: 10px;
        }
        
        .file-info {
            flex: 1;
        }
        
        .file-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 12px;
            color: #63656e;
            margin-bottom: 8px;
        }
        
        .file-name {
            flex: 1;
        }
        
        .file-actions {
            display: flex;
            align-items: center;
            gap: 8px;
            
            .file-size,
            .upload-progress {
                font-weight: bold;
            }
            
            .icon-delete {
                cursor: pointer;
                font-size: 14px;
                
                &:hover {
                    color: #ea3636;
                }
            }
        }
        
        .progress-bar {
            width: 100%;
            height: 4px;
            background: #dcdee5;
            border-radius: 2px;
            overflow: hidden;
            
            .progress-inner {
                height: 100%;
                background: #3a84ff;
                transition: width 0.3s;
            }
        }
        
        .upload-success {
            color: #43d068;
            font-size: 12px;
            
            .bk-icon {
                font-size: 20px;
                vertical-align: middle;
                margin-right: 4px;
            }
        }
    }
}
</style>

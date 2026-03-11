<template>
    <div
        v-bkloading="{ isLoading: isLoading }"
        class="fill-info-container"
    >
        <div class="fill-info-content">
            <bk-alert
                type="info"
                :title="$t('store.缺省时仅在调试云桌面下可用。设置后，可见范围内的用户可以安装使用应用。')"
                :closable="true"
            ></bk-alert>
            
            <div class="form-section">
                <span class="section-label">{{ $t('store.可见范围') }}</span>
                <div class="section-content">
                    <div
                        v-if="tagTestList.length"
                        class="tag-list"
                    >
                        <bk-tag
                            v-for="item in tagTestList"
                            :key="item.deptId"
                            :closable="!isEdit"
                            @close="handleTagClose(item.deptId)"
                        >
                            {{ item.deptName }}
                        </bk-tag>
                    </div>
                    
                    <div class="add-section">
                        <p
                            class="add-btn"
                            @click="handleAdd"
                        >
                            <i class="bk-icon icon-plus"></i>
                            <span>{{ $t('store.添加') }}</span>
                        </p>
                    </div>
                </div>
            </div>

            <div class="form-section printscreen">
                <span class="section-label">{{ $t('store.截图') }}</span>
                <div class="section-content">
                    <div class="upload-section">
                        <bk-upload
                            theme="picture"
                            :url="uploadUrl"
                            :accept="'image/png,image/jpeg,image/jpg,image/gif,image/svg+xml'"
                            :handle-res-code="handleResCode"
                            multiple
                            name="file"
                            :size="2"
                            :limit="6"
                            :files="filesList"
                            :tip="$t('store.请上传2-6张截图，每张不超过2M')"
                            @on-delete="handlePictureDelete"
                            @on-error="handleUploadError"
                            @on-exceed="handleExceed"
                        >
                        </bk-upload>
                        <div
                            v-if="errMessage"
                            class="error-message"
                        >
                            {{ $t('store.请上传2-6张截图，每张不超过2M') }}
                        </div>
                    </div>
                </div>
            </div>

            <div class="form-section">
                <span class="section-label">{{ $t('store.视频教程') }}</span>
                <div class="section-content">
                    <div class="video-list">
                        <div
                            v-for="item in videoList"
                            :key="item.tempId || item.mediaUrl"
                            class="video-item"
                        >
                            <div
                                v-if="item.loading"
                                v-bkloading="{ isLoading: true, size: 'mini' }"
                                class="video-loading"
                            ></div>
                            <template v-else>
                                <video
                                    controls
                                    playsinline
                                    :src="item.mediaUrl"
                                    class="video-player"
                                ></video>
                                <i
                                    class="bk-icon icon-close video-delete-icon"
                                    @click="handleVideoDelete(item.mediaUrl)"
                                ></i>
                            </template>
                        </div>
                        
                        <div
                            class="video-upload-trigger"
                            @click="triggerFileInput"
                        >
                            <input
                                ref="fileInput"
                                type="file"
                                multiple
                                accept="video/webm,video/ogg,video/mp4"
                                style="display: none"
                                @change="handleFileChange"
                            />
                            <i class="bk-icon icon-plus"></i>
                            <p class="upload-text">{{ $t('store.点击上传') }}</p>
                        </div>
                    </div>
                    <p class="tips">{{ $t('store.支持mp4、ogg、webm格式，不超过4个,每个不超过50M') }}</p>
                </div>
            </div>
        </div>

        <div
            v-if="isEdit"
            class="edit-mask"
        ></div>

        <div class="bottom-actions">
            <bk-button
                theme="primary"
                :disabled="isEdit"
                @click="goStep('next')"
            >
                {{ $t('store.下一步') }}
            </bk-button>
            <bk-button
                :disabled="isEdit"
                @click="goStep('prev')"
            >
                {{ $t('store.上一步') }}
            </bk-button>
        </div>

        <visible-range-dialog
            :show-dialog="isShowDialog"
            :select-data="tagTestList"
            :is-loading="isDialogLoading"
            @saveHandle="handleConfirm"
            @cancelHandle="handleCancel"
            @update="handleUpdate"
        ></visible-range-dialog>
    </div>
</template>

<script setup name="FillInInformationProgress">
    import { ref, computed, onMounted, onUnmounted } from 'vue'
    import UseInstance from '@/hook/useInstance.js'
    import VisibleRangeDialog from '@/components/VisibleRangeDialog.vue'
    import leaveConfirm from '@/utils/leave-confirm.js'

    const props = defineProps({
        appDetail: {
            type: Object,
            required: true
        },
        currentStep: {
            type: Object,
            required: true
        },
        isEdit: {
            type: Boolean,
            required: true
        }
    })

    const emit = defineEmits(['goStep'])

    const { proxy } = UseInstance()
    const { $store, $route, $t, $bkInfo, $bkMessage } = proxy

    const fileInput = ref(null)
    const isShowDialog = ref(false)
    const isDialogLoading = ref(false)
    const isLoading = ref(false)
    const errMessage = ref(false)
    
    const tagTestList = ref([])
    const initTagList = ref('')
    const filesList = ref([])
    const videoList = ref([])
    const pictureList = ref([])
    const initMediaInfoList = ref('')

    const storeCode = computed(() => $route.params.storeCode || props.appDetail?.storeCode)
    const storeId = computed(() => $route.params.storeId || props.appDetail?.storeId)
    const uploadUrl = '/ms/misc/api/user/file/upload'
    
    const mediaInfoList = computed(() => [...videoList.value, ...pictureList.value])
    const isEditing = computed(() =>
        initTagList.value !== tagTestList.value.map(item => item.deptId).toString()
        || initMediaInfoList.value !== mediaInfoList.value.map(item => item.mediaUrl).toString()
    )

    const init = () => {
        tagTestList.value = []
        filesList.value = []
        videoList.value = []
        pictureList.value = []
    }

    const getInformation = async () => {
        if (!storeCode.value) return
        
        try {
            isLoading.value = true
            const deptStatusInfos = 'APPROVING,APPROVED'
            const [visibilitieSInfo, mediaInfo] = await Promise.all([
                $store.dispatch('store/visibilitiesGet', {
                    storeCode: storeCode.value,
                    deptStatusInfos
                }),
                $store.dispatch('store/mediaInfoGet', storeCode.value)
            ])

            tagTestList.value = visibilitieSInfo.deptInfos || []

            if (mediaInfo && Array.isArray(mediaInfo)) {
                mediaInfo.forEach(({ mediaUrl, mediaType }) => {
                    if (mediaType === 'PICTURE') {
                        filesList.value.push({ url: mediaUrl })
                        pictureList.value.push({ mediaUrl, mediaType })
                    } else if (mediaType === 'VIDEO') {
                        videoList.value.push({ mediaUrl, mediaType })
                    }
                })
            }
            
            initTagList.value = tagTestList.value.map(item => item.deptId).toString()
            initMediaInfoList.value = mediaInfo?.map(item => item.mediaUrl).toString() || ''
        } catch (error) {
            console.error('获取信息失败:', error)
        } finally {
            isLoading.value = false
        }
    }

    const handleAdd = () => {
        if (!props.isEdit) {
            isShowDialog.value = true
        }
    }

    const handleTagClose = (deptId) => {
        tagTestList.value = tagTestList.value.filter(item => item.deptId !== deptId)
    }

    const goStep = async (status) => {
        if (pictureList.value.length < 2 && status === 'next') {
            errMessage.value = true
            return
        }
        
        try {
            errMessage.value = false
            if (status === 'prev' && isEditing.value) {
                const confirmed = await leaveConfirm($bkInfo, $t)
                if (!confirmed) {
                    return
                }
            }
            
            if (status === 'next') {
                const params = {
                    deptInfoList: tagTestList.value,
                    mediaInfoList: mediaInfoList.value.map(({ loading, tempId, ...rest }) => rest)
                }
                await $store.dispatch('store/progressInfoEdit', {
                    storeId: storeId.value,
                    params
                })
            }
            
            emit('goStep', props.currentStep, status)
        } catch (error) {
            console.error(error)
            $bkMessage({
                theme: 'error',
                message: error.message || $t('store.操作失败')
            })
        }
    }

    const handleConfirm = (params) => {
        const deptInfos = params.deptInfos || []
        deptInfos.forEach(newItem => {
            if (!tagTestList.value.some(existingItem => existingItem.deptId == newItem.deptId)) {
                tagTestList.value.push(newItem)
            }
        })
        isShowDialog.value = false
    }

    const handleUpdate = (deptInfos) => {
        tagTestList.value = deptInfos
    }

    const handleCancel = () => {
        isShowDialog.value = false
    }

    const handlePictureDelete = (file) => {
        pictureList.value = pictureList.value.filter(item => item.mediaUrl !== file.url)
    }

    const handleResCode = (res) => {
        if (res.data) {
            pictureList.value.push({
                mediaUrl: res.data,
                mediaType: 'PICTURE'
            })
            return true
        }
        if (res.message) {
            showMessage('error', res.message)
        }
        return false
    }

    const handleUploadError = (file, fileList) => {
        showMessage('error', $t('store.上传失败'))
    }

    const handleExceed = () => {
        showMessage('error', $t('store.最多上传6张截图'))
    }

    const triggerFileInput = () => {
        fileInput.value?.click()
    }

    const handleFileChange = async (event) => {
        const files = event.target.files
        const maxSize = 50 * 1024 * 1024

        if ((videoList.value.length + files.length) > 4) {
            showMessage('error', $t('store.最多只能上传4个视频'))
            return
        }

        // 使用 Promise.all 并行上传所有文件
        const uploadPromises = Array.from(files).map(async (file) => {
            if (file.size > maxSize) {
                showMessage('error', $t('store.文件超过了50MB的限制', { name: file.name }))
                return null
            }

            // 创建唯一标识
            const tempId = `temp_${Date.now()}_${Math.random()}`
            const placeholder = {
                mediaUrl: URL.createObjectURL(file),
                mediaType: 'VIDEO',
                loading: true,
                tempId
            }
            videoList.value.push(placeholder)

            const formData = new FormData()
            formData.append('file', file)

            try {
                const res = await $store.dispatch('store/uploadFiles', formData)
                const mediaUrl = res.data || res
                
                // 使用 tempId 查找并替换
                const index = videoList.value.findIndex(item => item.tempId === tempId)
                if (index !== -1) {
                    videoList.value.splice(index, 1, { mediaUrl, mediaType: 'VIDEO', loading: false })
                }
                return true
            } catch (error) {
                // 使用 tempId 删除失败的项
                videoList.value = videoList.value.filter(item => item.tempId !== tempId)
                showMessage('error', $t('store.上传失败，请重试'))
                return null
            }
        })

        await Promise.all(uploadPromises)
        event.target.value = ''
    }

    const handleVideoDelete = (fileUrl) => {
        videoList.value = videoList.value.filter(item => item.mediaUrl !== fileUrl)
    }

    const showMessage = (theme, message) => {
        $bkMessage({
            theme,
            message
        })
    }

    onMounted(() => {
        getInformation()
    })

    onUnmounted(() => {
        init()
    })
</script>

<style lang="scss" scoped>
.fill-info-container {
    position: relative;
    height: 100%;
    
    .fill-info-content {
        height: calc(100% - 56px);
        overflow-y: auto;
    }

    .printscreen {
      position: relative;

      .section-label::after {
        position: absolute;
        top: 0;
        width: 14px;
        color: #ea3636;
        text-align: center;
        content: '*';
      }
    }
    
    .form-section {
        display: flex;
        margin: 20px 0;
        
        .section-label {
            width: 100px;
            font-size: 14px;
            color: #666;
            flex-shrink: 0;
        }
        
        .section-content {
            flex: 1;
            
            .tag-list {
                display: flex;
                flex-wrap: wrap;
                gap: 8px;
                margin-bottom: 16px;
                
                ::v-deep .bk-tag {
                    font-size: 14px;
                    color: #222;
                    height: 32px;
                    line-height: 32px;
                    border-radius: 2px;
                }
            }
            
            .add-section {
                .add-btn {
                    color: #1592FF;
                    cursor: pointer;
                    font-size: 14px;
                    display: inline-flex;
                    align-items: center;
                    
                    i {
                        margin-right: 4px;
                    }
                    
                    &:hover {
                        opacity: 0.8;
                    }
                }
            }
            
            .upload-section {
                .upload-tip-wrapper {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    width: 100%;
                    height: 100%;
                    
                    .upload-plus-icon {
                        display: inline-block;
                        background-color: #1A6DF3;
                        color: #fff;
                        border-radius: 50%;
                        width: 24px;
                        height: 24px;
                        line-height: 24px;
                        text-align: center;
                    }
                    
                    .upload-tip-text {
                        text-align: center;
                        font-size: 14px;
                        color: #222;
                        margin-top: 15px;
                    }
                }
                
                .error-message {
                    font-size: 12px;
                    color: #ea3636;
                    margin-top: 8px;
                }
            }
            
            .video-list {
                display: flex;
                flex-wrap: wrap;
                gap: 8px;
                
                .video-item {
                    position: relative;
                    display: inline-block;
                    
                    .video-loading {
                        width: 240px;
                        height: 136px;
                        border: 1px solid #d9d9d9;
                        border-radius: 5px;
                    }
                    
                    .video-player {
                        width: 240px;
                        height: 136px;
                        border: 1px solid #d9d9d9;
                        border-radius: 5px;
                    }
                    
                    .video-delete-icon {
                        position: absolute;
                        top: 8px;
                        right: 8px;
                        border-radius: 50%;
                        font-size: 14px;
                        color: #fff;
                        background-color: #666;
                        padding: 4px;
                        cursor: pointer;
                        display: none;
                        
                        &:hover {
                            background-color: #333;
                        }
                    }
                    
                    &:hover .video-delete-icon {
                        display: block;
                    }
                }
                
                .video-upload-trigger {
                    display: inline-flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    width: 240px;
                    height: 136px;
                    border: 1px dashed #c4c6cc;
                    border-radius: 2px;
                    background-color: #fafbfd;
                    cursor: pointer;
                    
                    &:hover {
                        border-color: #3a84ff;
                    }
                    
                    i {
                        display: inline-block;
                        border-radius: 50%;
                        border: 1px solid #B8BCC8;
                        width: 24px;
                        height: 24px;
                        line-height: 24px;
                        text-align: center;
                    }
                    
                    .upload-text {
                        text-align: center;
                        font-size: 14px;
                        color: #666;
                        margin-top: 15px;
                    }
                }
            }

            .tips {
                color: #63656e;
                font-size: 12px;
                margin: 5px 0 10px;
                text-align: left;
            }
        }
    }
    
    .edit-mask {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
         height: calc(100% - 56px);
        cursor: not-allowed;
        z-index: 10;
    }
    
    .bottom-actions {
        position: absolute;
        bottom: 0;
        left: 0;
        width: 100%;
        
        .bk-button {
            height: 32px;
            font-size: 14px;
            
            & + .bk-button {
                margin-left: 24px;
            }
        }
    }
}
</style>

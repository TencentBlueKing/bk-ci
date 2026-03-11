<template>
    <div class="version-info-component">
        <bk-form-item
            :label="$t('store.发布者')"
            property="publisher"
            error-display-type="normal"
            :required="true"
        >
            <bk-select
                v-model="versionInfo.publisher"
                :clearable="true"
                style="width: 400px"
                @change="val => updateField('publisher', val)"
            >
                <bk-option
                    v-for="item in publisherList"
                    :key="item.id"
                    :id="item.publisherCode"
                    :name="item.publisherName"
                />
            </bk-select>
        </bk-form-item>
        
        <bk-form-item
            :label="$t('store.版本号')"
            property="version"
            :required="true"
            error-display-type="normal"
        >
            <div class="version-input-wrapper">
                <bk-input
                    v-model="versionInfo.version"
                    :disabled="uploading"
                    style="width: 400px"
                    @input="handleVersionInput"
                />
                <span
                    v-if="versionInfo.lastVersion"
                    class="last-version-tip"
                >
                    {{ $t('store.上次发布版本：') }}{{ versionInfo.lastVersion }}
                </span>
            </div>
        </bk-form-item>
        
        <bk-form-item
            :label="$t('store.版本日志')"
            property="versionContent"
            :required="true"
            error-display-type="normal"
        >
            <mavon-editor
                v-model="versionInfo.versionContent"
                :toolbars="toolbars"
                :external-link="false"
                :box-shadow="false"
                preview-background="#fff"
                :language="$i18n.locale === 'en-US' ? 'en' : $i18n.locale"
                @change="handleVersionContentChange"
            />
        </bk-form-item>
    </div>
</template>

<script setup>
    import { computed, onMounted, ref } from 'vue'
    import UseInstance from '@/hook/useInstance.js'
    import { toolbars } from '@/utils/editor-options'

    const props = defineProps({
        versionInfo: {
            type: Object,
            default: () => ({})
        },
        hasPackage: {
            type: Boolean,
            default: false
        },
        uploading: {
            type: Boolean,
            default: false
        },
        storeCode: {
            type: String,
            required: true
        },
        packageVersion: {
            type: String,
            default: ''
        }
    })

    const emit = defineEmits(['update:versionInfo', 'inputChange', 'updateReleaseType'])

    const { proxy } = UseInstance()
    const { $store, $bkInfo, $t } = proxy

    const publisherList = ref([])

    const isCanceled = computed(() => props.versionInfo.releaseType === 'CANCEL_RE_RELEASE')

    // 处理版本日志变化
    const handleVersionContentChange = (value, render) => {
        updateField('versionContent', value)
    }

    // 更新字段的辅助函数
    const updateField = (field, value) => {
        const newVersionInfo = {
            ...props.versionInfo,
            [field]: value
        }
        emit('update:versionInfo', newVersionInfo)
        
        // 处理版本号变化时的 releaseType 更新
        if (field === 'version' && isCanceled.value) {
            const { lastVersion } = props.versionInfo
            const newReleaseType = value !== lastVersion
                ? 'COMPATIBILITY_FIX'
                : 'CANCEL_RE_RELEASE'
            emit('updateReleaseType', newReleaseType)
        }
    }

    onMounted(() => {
        fetchPublisherList()
    })

    async function fetchPublisherList () {
        try {
            publisherList.value = await $store.dispatch('store/getPublisherList', props.storeCode)
        } catch (error) {
            console.error(error)
        }
    }

    function handleVersionInput (value) {
        if (props.hasPackage) {
            $bkInfo({
                title: $t('store.确认要切换类型吗？'),
                subTitle: $t('store.类型切换需要重新上传软件安装包，是否继续'),
                okText: $t('store.确认'),
                cancelText: $t('store.取消'),
                confirmFn: () => {
                    updateField('version', value)
                    emit('inputChange')
                },
                cancelFn: () => {
                    updateField('version', props.packageVersion)
                    console.log("🚀 ~ handleVersionInput ~ props.packageVersion:", props.packageVersion)
                }
            })
        } else {
            updateField('version', value)
        }
    }
</script>

<style lang="scss" scoped>
.version-info-component {
    .version-input-wrapper {
        display: flex;
        align-items: center;
        gap: 8px;
    }
    
    .last-version-tip {
        font-size: 12px;
        color: #979ba5;
        margin-left: 8px;
    }

}
::v-deep .v-note-wrapper {
    height: 263px;
    border: 1px solid #c4c6cc;
    
    .v-note-panel {
        box-shadow: none;
    }
}
</style>

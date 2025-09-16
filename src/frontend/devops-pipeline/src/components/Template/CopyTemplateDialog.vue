<template>
    <bk-dialog
        width="800"
        v-model="copyTemp.isShow"
        header-position="left"
        ext-cls="form-dialog"
        :title="copyTemp.title"
        :close-icon="copyTemp.closeIcon"
        @confirm="copyConfirmHandler"
        @cancel="copyCancelHandler"
    >
        <template>
            <section class="copy-pipeline bk-form">
                <div class="bk-form-item">
                    <label class="bk-label template-name-copy">
                        {{ $t('template.name') }}
                    </label>
                    <div class="bk-form-content">
                        <input
                            type="text"
                            class="bk-form-input"
                            :placeholder="$t('template.nameInputTips')"
                            v-model="copyTemp.templateName"
                            :class="{ 'is-danger': copyTemp.nameHasError }"
                            @input="copyTemp.nameHasError = false"
                            name="copyTemplateName"
                            v-validate="&quot;required|max:30&quot;"
                            maxlength="30"
                        >
                    </div>
                    <p
                        v-if="errors.has('copyTemplateName')"
                        class="error-tips err-name"
                    >
                        {{ $t('template.nameErrTips') }}
                    </p>
                </div>

                <div class="bk-form-item">
                    <label class="bk-label tip-bottom">{{ $t('template.applySetting') }}
                        <span
                            v-bk-tooltips.bottom-end="$t('template.tipsSetting')"
                            class="bottom-end"
                        >
                            <i class="bk-icon icon-info-circle"></i>
                        </span>
                    </label>
                    <div class="bk-form-content">
                        <bk-radio-group v-model="copyTemp.isCopySetting">
                            <bk-radio
                                v-for="(entry, key) in copySettings"
                                :key="key"
                                :value="entry.value"
                                class="form-radio"
                            >
                                {{ entry.label }}
                            </bk-radio>
                        </bk-radio-group>
                    </div>
                </div>
            </section>
        </template>
    </bk-dialog>
</template>

<script setup>
    import { ref, defineProps, defineEmits } from 'vue'
    import UseInstance from '@/hook/useInstance'

    const { t } = UseInstance()
    defineProps({
        copyTemp: {
            type: Object,
            default: () => ({
                isShow: false,
                title: '',
                closeIcon: false,
                quickClose: true,
                padding: '0 20px',
                srcTemplateId: '',
                templateName: '',
                isCopySetting: true
            })
        }
    })
    const copySettings = ref([
        { label: t('true'), value: true },
        { label: t('false'), value: false }
    ])

    const emit = defineEmits(['confirm', 'cancel'])

    async function copyConfirmHandler () {
        emit('confirm')
    }

    function copyCancelHandler () {
        emit('cancel')
    }
</script>

<style lang="scss" scoped>
.form-dialog {
    .err-name {
        text-align: left;
        margin-left: 150px;
        margin-bottom: -21px;
    }
    .form-radio {
        margin-right: 30px;
    }
    .copy-pipeline .template-name-copy {
        text-align: right;
        padding-right: 40px;
        font-weight: 400;
    }
    .tip-bottom {
        font-weight: 400;
    }
}
</style>

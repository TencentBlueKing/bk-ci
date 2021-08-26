<template>
    <section class="g-turbo-box task-setting">
        <h3 class="create-title g-turbo-deep-black-font" @click="showIPSetting = !showIPSetting">
            <logo name="right-shape" size="16" :class="showIPSetting ? 'right-down right-shape' : 'right-shape'"></logo>
            {{ $t('turbo.设置') }}
        </h3>
        <bk-form :label-width="138" :model="copyFormData" v-if="showIPSetting">
            <bk-form-item :label="$t('turbo.IP白名单')" property="ip">
                <template v-if="isEdit">
                    <bk-input type="textarea"
                        class="double-width"
                        :rows="3"
                        :maxlength="100"
                        v-model="copyFormData.whiteList">
                    </bk-input>
                    <p class="ip-tip"> {{ $t('turbo.默认为“0.0.0.0”，即不限制加速任务来源。若需限制来源，请在此填写允许的来源IP，多个以英文分号分隔。') }} </p>
                </template>
                <span v-else class="g-turbo-text-break">{{ formData.whiteList }}</span>
            </bk-form-item>
        </bk-form>
        <bk-button v-if="isEdit && !onlyEdit" theme="primary" class="g-turbo-bottom-button" @click="save"> {{ $t('turbo.保存') }} </bk-button>
        <bk-button v-if="isEdit && !onlyEdit" class="g-turbo-bottom-button" @click="cancel"> {{ $t('turbo.取消') }} </bk-button>
        <span class="g-turbo-edit-button" @click="(isEdit = true, showIPSetting = true)" v-if="!onlyEdit && !isEdit"><logo name="edit" size="16"></logo> {{ $t('turbo.编辑') }} </span>
    </section>
</template>

<script>
    import { modifyTaskWhiteList } from '@/api'
    import logo from '@/components/logo'

    export default {
        components: {
            logo
        },

        props: {
            onlyEdit: {
                type: Boolean,
                default: true
            },
            formData: {
                type: Object
            }
        },

        data () {
            return {
                isEdit: this.onlyEdit,
                copyFormData: {},
                isLoadng: false,
                showIPSetting: false
            }
        },

        created () {
            this.copyFormData = JSON.parse(JSON.stringify(this.formData))
        },

        methods: {
            save () {
                this.isLoadng = true
                modifyTaskWhiteList(this.copyFormData).then(() => {
                    this.$bkMessage({ theme: 'success', message: this.$t('turbo.修改成功') })
                    this.$emit('update:formData', this.copyFormData)
                    this.isEdit = false
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoadng = false
                })
            },

            cancel () {
                this.copyFormData = JSON.parse(JSON.stringify(this.formData))
                this.isEdit = false
            }
        }
    }
</script>

<style lang="scss" scoped>
    .task-setting {
        position: relative;
    }

    .g-turbo-box {
        margin-bottom: 20px;
        padding: 26px 32px;
        .create-title {
            font-size: 14px;
            line-height: 22px;
            margin-bottom: 17px;
            display: flex;
            align-items: center;
            cursor: pointer;
        }
    }

    .double-width {
        width: 7.1rem;
    }

    .right-shape {
        transition: 200ms transform;
        transform: rotate(0deg);
        color: #979ba5;
        margin-left: -16px;
        margin-right: 5px;
        &.right-down {
            transform: rotate(90deg);
        }
    }

    .ip-tip {
        margin-top: 4px;
        line-height: 20px;
        font-size: 12px;
        color: #979BA5;
    }
</style>

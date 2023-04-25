<template>
    <section v-bkloading="{ isLoading }">
        <bk-form :label-width="100" :model="formData" class="manage-detail-edit" ref="editForm" v-if="!isLoading">
            <bk-form-item :label="$t('store.微扩展名称')" :required="true" property="serviceName" :rules="[requireRule($t('store.微扩展名称')), numMax(20), nameRule]" ref="serviceName" error-display-type="normal">
                <bk-input v-model="formData.serviceName" :placeholder="$t('store.请输入微扩展名称，不超过20个字符')"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('store.扩展点')" :required="true" property="extensionItemList" :rules="[requireRule($t('store.扩展点'))]" ref="extensionItemList" error-display-type="normal">
                <bk-select :placeholder="$t('store.请选择扩展点')"
                    class="service-item"
                    :scroll-height="300"
                    :clearable="true"
                    @toggle="getServiceList"
                    :loading="isServiceListLoading"
                    searchable
                    multiple
                    display-tag
                    v-model="formData.extensionItemList">
                    <bk-option-group
                        v-for="(group, index) in serviceList"
                        :name="group.name"
                        :key="index">
                        <bk-option v-for="(option, key) in group.children"
                            :key="key"
                            :id="option.id"
                            :name="option.name"
                        >
                        </bk-option>
                    </bk-option-group>
                </bk-select>
            </bk-form-item>
            <bk-form-item :label="$t('store.标签')" property="labelIdList">
                <bk-tag-input v-model="formData.labelIdList" :list="labelList" display-key="labelName" search-key="labelName" trigger="focus" :placeholder="$t('store.请选择标签')"></bk-tag-input>
            </bk-form-item>
            <bk-form-item :label="$t('store.简介')" property="summary" :required="true" :rules="[requireRule($t('store.简介'))]" ref="summary" error-display-type="normal">
                <bk-input v-model="formData.summary" :placeholder="$t('store.请输入简介')"></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('store.描述')" property="description">
                <bk-radio-group v-model="formData.descInputType">
                    <bk-radio value="MANUAL" class="service-input"> {{ $t('store.手动录入') }} </bk-radio>
                    <bk-radio value="FILE"> {{ $t('store.fromReadme') }} </bk-radio>
                </bk-radio-group>
                <mavon-editor class="remark-input"
                    v-if="formData.descInputType === 'MANUAL'"
                    ref="mdHook"
                    v-model="formData.description"
                    :toolbars="toolbars"
                    :external-link="false"
                    :box-shadow="false"
                    preview-background="#fff"
                    @imgAdd="uploadimg"
                />
            </bk-form-item>
            <bk-form-item :label="$t('store.截图')">
                <upload type="PICTURE"
                    :file-list.sync="imageList"
                    :limit="6"
                    :size="2"
                    :tip="$t('store.支持jpg、png、gif、svg格式，不超过6张，每张不超过2M')"
                ></upload>
            </bk-form-item>
            <bk-form-item :label="$t('store.视频教程')">
                <upload type="VIDEO"
                    :file-list.sync="videoList"
                    :limit="4"
                    :size="50"
                    :tip="$t('store.支持mp4、ogg、webm格式，不超过4个，每个不超过50M')"
                ></upload>
            </bk-form-item>
            <bk-form-item :required="true" property="logoUrl" error-display-type="normal" class="edit-logo">
                <select-logo :form="formData" type="SERVICE" :is-err="false" ref="logoUrlError"></select-logo>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" @click="save" :loading="isSaving">{{ $t('store.保存') }}</bk-button>
                <bk-button :disabled="isSaving" @click="$router.back()">{{ $t('store.取消') }}</bk-button>
            </bk-form-item>
        </bk-form>
    </section>
</template>

<script>
    import { mapActions } from 'vuex'
    import { toolbars } from '@/utils/editor-options'
    import selectLogo from '@/components/common/selectLogo'
    import upload from '@/components/upload'

    export default {
        components: {
            selectLogo,
            upload
        },

        props: {
            detail: Object
        },
        
        data () {
            return {
                formData: JSON.parse(JSON.stringify(this.detail)),
                nameRule: {
                    validator: (val) => (/^[\u4e00-\u9fa5a-zA-Z0-9-]*$/.test(val)),
                    message: this.$t('store.由汉字、英文字母、数字、连字符(-)组成，长度小于20个字符'),
                    trigger: 'blur'
                },
                serviceList: [],
                labelList: [],
                imageList: [],
                videoList: [],
                isServiceListLoading: false,
                toolbars,
                isLoading: true,
                isSaving: false,
                hasChange: false
            }
        },

        watch: {
            formData: {
                handler () {
                    if (!this.isLoading) this.hasChange = true
                },
                deep: true
            }
        },

        created () {
            this.initData()
        },

        methods: {
            ...mapActions('store', [
                'requestServiceItemList',
                'requestServiceLabel',
                'requestUpdateServiceInfo'
            ]),

            initData () {
                Promise.all([
                    this.requestServiceLabel(),
                    this.hackData(),
                    this.getServiceList(true)
                ]).then(([labels]) => {
                    this.labelList = labels || []
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoading = false
                })
            },

            hackData () {
                this.formData.labelIdList = (this.formData.labelList || []).map(label => label.id)
                this.formData.description = this.formData.description || this.$t('store.serviceMdDesc')
                this.$set(this.formData, 'desType', this.formData.desType || 'hand')
                const mediaList = this.formData.mediaList || []
                this.imageList.push(...mediaList.filter(x => x.mediaType === 'PICTURE'))
                this.videoList.push(...mediaList.filter(x => x.mediaType === 'VIDEO'))
                return Promise.resolve()
            },

            getServiceList (isExpand) {
                if (!isExpand) return
                const code = this.formData.projectCode
                this.isServiceListLoading = true
                this.requestServiceItemList(code).then((res) => {
                    this.serviceList = (res || []).map((item) => {
                        const serviceItem = item.extServiceItem || {}
                        return {
                            name: serviceItem.name,
                            children: item.childItem || []
                        }
                    })
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => (this.isServiceListLoading = false))
            },

            requireRule (name) {
                return {
                    required: true,
                    message: this.$t('store.validateMessage', [name, this.$t('store.必填项')]),
                    trigger: 'blur'
                }
            },

            numMax (num) {
                return {
                    validator: (val = '') => (val.length <= num),
                    message: this.$t('store.validateNum', [num]),
                    trigger: 'blur'
                }
            },

            save () {
                this.$refs.editForm.validate().then(() => {
                    this.isSaving = true

                    this.formData.mediaList = [...this.imageList, ...this.videoList]
                    const postData = {
                        serviceCode: this.formData.serviceCode,
                        data: this.formData
                    }
                    this.requestUpdateServiceInfo(postData).then(() => {
                        return this.$store.dispatch('store/requestServiceDetailByCode', postData.serviceCode).then((res) => {
                            this.$store.dispatch('store/clearDetail')
                            this.$store.dispatch('store/setDetail', res || {})
                            this.$bkMessage({ message: this.$t('store.修改成功'), theme: 'success' })
                            this.hasChange = false
                            this.$router.back()
                        })
                    }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                        this.isSaving = false
                    })
                }, (validator) => {
                    this.$bkMessage({ message: validator.content || validator, theme: 'error' })
                })
            },

            async uploadimg (pos, file) {
                const formData = new FormData()
                const config = {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                }
                let message, theme
                formData.append('file', file)

                try {
                    const res = await this.$store.dispatch('store/uploadFile', {
                        formData,
                        config
                    })

                    this.$refs.mdHook.$img2Url(pos, res)
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                    this.$refs.mdHook.$refs.toolbar_left.$imgDel(pos)
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .service-input {
        margin-right: 21px;
    }
    .remark-input {
        margin-top: 15px;
    }
</style>

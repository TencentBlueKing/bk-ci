<template>
    <article class="edit-atom-wrapper edit-detail" v-bkloading="{ isLoading }">
        <section class="inner-header">
            <div class="title"> {{ $t('镜像编辑') }} </div>
        </section>

        <main class="edit-main">
            <bk-form ref="editForm" class="edit-image" label-width="125" :model="form">
                <bk-form-item class="wt660" :label="$t('镜像名称')" :required="true" property="imageName" :rules="[requireRule]" ref="imageName">
                    <bk-input v-model="form.imageName" :placeholder="$t('请输入镜像名称')"></bk-input>
                </bk-form-item>
                <bk-form-item class="wt660" :label="$t('分类')" :required="true" property="classifyCode" :rules="[requireRule]" ref="classifyCode">
                    <bk-select v-model="form.classifyCode" searchable>
                        <bk-option v-for="(option, index) in classifys"
                            :key="index"
                            :id="option.classifyCode"
                            :name="option.classifyName"
                            :placeholder="$t('请选择分类')"
                        >
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item class="wt660" :label="$t('标签')" property="labelIdList">
                    <bk-select v-model="form.labelIdList" searchable multiple show-select-all>
                        <bk-option v-for="(option, index) in labelList"
                            :key="index"
                            :id="option.id"
                            :name="option.labelName"
                            :placeholder="$t('请选择功能标签')"
                        >
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item :label="$t('范畴')" property="category" :required="true" :rules="[requireRule]" ref="category">
                    <bk-select v-model="form.category" searchable>
                        <bk-option v-for="(option, index) in categoryList"
                            :key="index"
                            :id="option.categoryCode"
                            :name="option.categoryName"
                            :placeholder="$t('请选择范畴')"
                        >
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item :label="$t('简介')" property="summary" :required="true" :rules="[requireRule]" ref="summary">
                    <bk-input v-model="form.summary" :placeholder="$t('请输入简介')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('描述')" property="description">
                    <mavon-editor class="image-remark-input"
                        ref="mdHook"
                        v-model="form.description"
                        :toolbars="toolbars"
                        :external-link="false"
                        :box-shadow="false"
                        preview-background="#fff"
                        @imgAdd="uploadimg"
                    />
                </bk-form-item>
                <div class="version-msg">
                    <p class="form-title"> {{ $t('版本信息') }} </p>
                    <hr class="cut-line">
                </div>
                <bk-form-item :label="$t('发布者')" :required="true" property="publisher" :rules="[requireRule]" ref="publisher">
                    <bk-input v-model="form.publisher" :placeholder="$t('请输入发布者')"></bk-input>
                </bk-form-item>
                <bk-form-item>
                    <bk-button theme="primary" @click.native="saveImage"> {{ $t('保存') }} </bk-button>
                </bk-form-item>
                <select-logo ref="selectLogo" label="Logo" :form="form" type="IMAGE" :is-err="logoErr" right="25"></select-logo>
            </bk-form>
        </main>
    </article>
</template>

<script>
    import { mapActions } from 'vuex'
    import { toolbars } from '@/utils/editor-options'
    import selectLogo from '@/components/common/selectLogo'

    export default {
        components: {
            selectLogo
        },

        data () {
            return {
                isLoading: false,
                form: JSON.parse(JSON.stringify(this.$store.state.store.currentImage)),
                requireRule: {
                    required: true,
                    message: this.$t('必填项'),
                    trigger: 'blur'
                },
                classifys: [],
                labelList: [],
                categoryList: [],
                toolbars
            }
        },

        created () {
            this.hackData()
            this.initData()
        },

        methods: {
            ...mapActions('store', [
                'requestImageClassifys',
                'requestImageLabel',
                'requestImageCategorys',
                'requestUpdateImageInfo'
            ]),

            hackData () {
                this.form.labelIdList = this.form.labelList.map(label => label.id)
                this.form.description = this.form.description || this.$t('### 功能简介\n\n### 如何使用\n\n### 注意事项\n\n### License')
            },

            initData () {
                this.isLoading = true
                Promise.all([
                    this.requestImageClassifys(),
                    this.requestImageLabel(),
                    this.requestImageCategorys()
                ]).then(([classifys, labels, categorys]) => {
                    this.classifys = classifys
                    this.labelList = labels
                    this.categoryList = categorys
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoading = false
                })
            },

            saveImage () {
                this.$refs.editForm.validate().then(() => {
                    if (!this.form.logoUrl) {
                        this.logoErr = true
                        const err = { field: 'selectLogo' }
                        throw err
                    }
                    this.isLoading = true
                    const postData = {
                        imageCode: this.form.imageCode,
                        data: this.form
                    }
                    this.requestUpdateImageInfo(postData).then(() => {
                        this.$bkMessage({ message: this.$t('修改成功'), theme: 'success' })
                        this.$router.push({ name: 'imageDetail' })
                    }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                        this.isLoading = false
                    })
                }).catch((validate) => {
                    const field = validate.field
                    const label = this.$refs[field].label
                    this.$bkMessage({ message: `${label + this.$t('是必填项，请填写以后重试')}`, theme: 'error' })
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
    @import './../../assets/scss/conf';
    .edit-atom-wrapper {
        height: 100%;
        overflow: auto;
        .inner-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                font-size: 16px;
            }
        }
    }

    .h32 {
        height: 32px;
    }

    .mt6 {
        margin-top: 6px;
    }

    .mr12 {
        margin-right: 12px;
    }

    .lh30 {
        line-height: 30px;
    }

    .wt660 {
        width: 660px;
    }

    .version-msg {
        margin: 30px 0 20px;
    }

    .edit-main {
        height: calc(100% - 60px);
        overflow: auto;
    }

    .edit-image {
        position: relative;
        width: 1200px;
        margin: 18px 20px;
        .image-remark-input {
            height: 263px;
            border: 1px solid #c4c6cc;
            &.fullscreen {
                height: auto;
            }
        }
    }
</style>

<template>
    <div class="main-content-inner main-content-form">
        <bk-form :label-width="120" :model="formData" ref="basicInfo">
            <bk-form-item :label="$t('basic.英文名称')" property="taskDetail.nameEn">
                <bk-input v-model="formData.nameEn" readonly></bk-input>
            </bk-form-item>
            <bk-form-item :label="$t('basic.中文名称')" :required="true" :rules="formRules.nameCn" property="nameCn">
                <bk-input v-model.trim="formData.nameCn"></bk-input>
            </bk-form-item>
            <divider />
            <bk-form-item :label="$t('basic.管理员')" property="taskOwner">
                <bk-input v-model.trim="formData.taskOwner" disabled></bk-input>
                <a :href="editUrl" class="modify-link" target="_blank" @click="updateOwner = true">
                    {{$t('op.修改')}} <i class="bk-icon icon-edit"></i>
                </a>
                <a href="javascript:void(0)" class="reload-link" v-if="updateOwner" @click="reload">{{$t('basic.已修改点此刷新')}}</a>
            </bk-form-item>
            <bk-form-item :label="$t('basic.任务成员')" property="taskMember">
                <bk-input v-model.trim="formData.taskMember" disabled></bk-input>
                <a :href="editUrl" class="modify-link" target="_blank" @click="updateMember = true">
                    {{$t('op.修改')}} <i class="bk-icon icon-edit"></i>
                </a>
                <a href="javascript:void(0)" class="reload-link" v-if="updateMember" @click="reload">{{$t('basic.已修改点此刷新')}}</a>
            </bk-form-item>
            <divider />
            <bk-form-item :label="$t('basic.任务语言')" :required="true" :rules="formRules.codeLang" property="codeLang">
                <bk-checkbox-group v-model="formData.codeLang" @change="handleLangChange" class="checkbox-lang">
                    <bk-checkbox v-for="lang in toolMeta.LANG" :key="lang.key" :value="parseInt(lang.key)" class="item">{{lang.name}}</bk-checkbox>
                </bk-checkbox-group>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" :loading="buttonLoading" :title="$t('op.保存')" @click.stop.prevent="submitData">{{$t("op.保存")}}</bk-button>
            </bk-form-item>
        </bk-form>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import Divider from '@/components/divider'

    export default {
        components: {
            Divider
        },
        data () {
            return {
                formData: {},
                formRules: {
                    nameCn: [
                        {
                            required: true,
                            message: this.$t('st.必填项'),
                            trigger: 'blur'
                        },
                        {
                            regex: /^[\u4e00-\u9fa5_a-zA-Z0-9]+$/,
                            message: this.$t('st.需由中文、字母、数字或下划线组成'),
                            trigger: 'blur'
                        },
                        {
                            max: 50,
                            message: this.$t('st.不能多于x个字符', { num: 50 }),
                            trigger: 'blur'
                        }
                    ],
                    codeLang: [
                        {
                            required: true,
                            message: this.$t('st.必填项'),
                            trigger: 'change'
                        }
                    ]
                },
                updateOwner: false,
                updateMember: false,
                buttonLoading: false,
                editUrl: `${window.PAAS_SERVICE_URL}/o/bk_iam_app/`
            }
        },
        computed: {
            ...mapState([
                'toolMeta'
            ])
        },
        watch: {},
        methods: {
            async fetchPageData () {
                const params = { taskId: this.$route.params.taskId }
                const res = await this.$store.dispatch('task/basicInfo', params)
                const restwo = await this.$store.dispatch('task/memberInfo')
                restwo.taskMember = restwo.taskMember.join()
                restwo.taskOwner = restwo.taskOwner.join()
                res.codeLang = this.toolMeta.LANG.map(lang => lang.key & res.codeLang).filter(lang => lang > 0)
                this.formData = Object.assign(res, restwo)
            },
            handleLangChange (newValue) {
                if (!newValue) {
                    newValue = this.formData.codeLang
                }
                const formItem = this.$refs.basicInfo.formItems[4]
                if (newValue.length) {
                    formItem.clearValidator()
                } else {
                    const validator = formItem.validator
                    const msg = this.$t('st.请选择至少一种任务语言')
                    setTimeout(function () {
                        validator.state = 'error'
                        validator.content = msg
                    }, 100)
                }
            },
            async reload () {
                await this.fetchPageData()
                this.updateOwner = this.updateMember = false
            },
            submitData () {
                this.handleLangChange()
                const formItems = this.$refs.basicInfo.formItems
                let hasError = false
                for (let index = 0; index < formItems.length; index++) {
                    if (formItems[index].validator && formItems[index].validator.state === 'error') hasError = true
                }

                if (!hasError) {
                    this.buttonLoading = true
                    this.$refs.basicInfo.validate().then(validator => {
                        const params = {
                            taskId: this.$route.params.taskId,
                            nameCn: this.formData.nameCn,
                            codeLang: String(this.formData.codeLang.reduce((n1, n2) => n1 + n2, 0))
                        }
                        this.$store.dispatch('task/updateBasicInfo', params).then(res => {
                            if (res === true) {
                                this.$bkMessage({
                                    theme: 'success',
                                    message: this.$t('op.修改成功')
                                })
                            }
                        }).catch(e => {
                            console.error(e)
                        }).finally(() => {
                            this.buttonLoading = false
                        })
                    }, validator => {
                        // console.log(validator)
                    })
                }
            }
        }
    }
</script>

<style>
</style>

<style lang="postcss" scoped>
    >>>.bk-form-input[readonly] {
        border: none 0;
        background: transparent!important;
    }
    >>>.bk-form .bk-form-content {
        min-width: 390px;
    }
    .checkbox-lang {
        .item {
            width: 130px;
        }
    }
    .modify-link {
        position: absolute;
        right: -70px;
        top: 6px;
        font-size: 14px;
    }
    .reload-link {
        position: absolute;
        right: -185px;
        top: 6px;
        font-size: 14px;
    }
</style>

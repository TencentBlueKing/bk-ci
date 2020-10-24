<template>
    <div>
        <bk-sideslider class="create-checkerset-wrapper" :title="title" width="560" :is-show.sync="visiable" @hidden="closeSlider">
            <div class="slider-content" slot="content">
                <bk-form class="checkerset-side-form" ref="checkersetForm" v-if="visiable" :model="newCheckersetParams" label-width="100">
                    <bk-form-item :label="$t('规则集名称')" :rules="formRules.checkerSetName" :required="true" property="checkerSetName">
                        <bk-input
                            :placeholder="$t('可使用中文、字母、数字、下划线，如：企鹅电竞前端规范')"
                            v-model="newCheckersetParams.checkerSetName">
                        </bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('规则集ID')" :rules="formRules.checkerSetId" :required="true" property="checkerSetId">
                        <bk-input
                            :disabled="!isListRouter"
                            :placeholder="$t('可使用字母、数字、下划线，如：penguin_web_standard')"
                            v-model="newCheckersetParams.checkerSetId">
                        </bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('适用语言')" :rules="formRules.codeLang" :required="true" property="codeLang">
                        <bk-select :z-index="3001" v-model="newCheckersetParams.codeLang" :disabled="isEdit">
                            <bk-option v-for="codeLang in codeLangs"
                                :key="codeLang.codeLang"
                                :id="codeLang.codeLang"
                                :name="codeLang.displayName">
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item :label="$t('描述')" :rules="formRules.description" :required="true" property="description">
                        <bk-input type="textarea" v-model="newCheckersetParams.description" placeholder="请输入描述"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('类型')" :rules="formRules.catagories" :required="true" property="catagories">
                        <bk-select v-model="newCheckersetParams.catagories" :disabled="isCopy">
                            <bk-option v-for="category in catatories"
                                :key="category.enName"
                                :id="category.enName"
                                :name="category.cnName">
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item :label="$t('基于')" v-if="isListRouter && !isCopy">
                        <bk-select v-model="newCheckersetParams.baseCheckerSetId" :disabled="isEdit">
                            <bk-option v-for="checkerset in basicCheckerSetList"
                                :key="checkerset.checkerSetId"
                                :id="checkerset.checkerSetId"
                                :name="checkerset.checkerSetName">
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                    <template v-if="hasDetail">
                        <bk-form-item desc="发布者创建规则集所在项目" :label="$t('源项目')">
                            <span class="fs12">{{projectName}}</span>
                        </bk-form-item>
                        <bk-form-item :label="$t('发布者')" class="mt10">
                            <span class="fs12">{{newCheckersetParams.creator || '--'}}</span>
                        </bk-form-item>
                        <bk-form-item :label="$t('更新时间')" class="mt10">
                            <span class="fs12">{{newCheckersetParams.lastUpdateTime | formatDate}}</span>
                        </bk-form-item>
                    </template>
                    <bk-form-item class="main-bottom">
                        <bk-button
                            v-bk-tooltips="{ content: hasPermission ? '' : $t('暂无规则集编辑权限，可在新建/复制规则集后进行修改。') }"
                            theme="primary"
                            :class="hasPermission ? '' : 'disable-save'"
                            :loading="isSubmitting"
                            @click.stop.prevent="submitData">
                            {{submitTxt}}
                        </bk-button>
                    </bk-form-item>
                </bk-form>
            </div>
        </bk-sideslider>
    </div>
</template>
<script>
    import axios from 'axios'

    export default {
        props: {
            visiable: {
                type: Boolean,
                default: false
            },
            isEdit: {
                type: Boolean,
                default: false
            },
            editObj: {
                type: Object,
                default () {
                    return {}
                }
            },
            refreshDetail: Function,
            hasPermission: {
                type: Boolean,
                default: true
            },
            hasDetail: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                submitTxt: this.isListRouter ? '创建' : '保存',
                isSubmitting: false,
                isCopy: false,
                codeLangs: [],
                catatories: [],
                checkerSets: [],
                newCheckersetParams: {
                    checkerSetName: '',
                    checkerSetId: '',
                    codeLang: '',
                    description: '',
                    catagories: '',
                    baseCheckerSetId: ''
                },
                formRules: {
                    checkerSetName: [
                        {
                            required: true,
                            message: '必填项',
                            trigger: 'blur'
                        },
                        {
                            regex: /^[a-zA-Z0-9_\u4e00-\u9fa5]{1,50}/,
                            message: '请以中文、字母、数字、下划线命名',
                            trigger: 'blur'
                        }
                    ],
                    checkerSetId: [
                        {
                            required: true,
                            message: '必填项',
                            trigger: 'blur'
                        },
                        {
                            regex: /^[0-9a-zA-Z_]{1,50}$/,
                            message: '请以字母、数字、下划线命名',
                            trigger: 'blur'
                        }
                    ],
                    codeLang: [
                        {
                            required: true,
                            message: this.$t('必填项'),
                            trigger: 'blur'
                        }
                    ],
                    description: [
                        {
                            required: true,
                            message: this.$t('必填项'),
                            trigger: 'blur'
                        }
                    ],
                    catagories: [
                        {
                            required: true,
                            message: this.$t('必填项'),
                            trigger: 'blur'
                        }
                    ]
                },
                projectName: '--'
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            isListRouter () {
                return this.$route.name === 'checkerset-list'
            },
            title () {
                return this.isListRouter ? this.isCopy ? '复制规则集' : '创建规则集' : '编辑规则集'
            },
            basicCheckerSetList () {
                let matchList = this.checkerSets || []
                if (this.newCheckersetParams.codeLang) {
                    matchList = matchList.filter(item => item.codeLang === this.newCheckersetParams.codeLang)
                }
                return matchList
            }
        },
        watch: {
            visiable (newVal) {
                if (newVal) {
                    const tempForm = {
                        checkerSetName: '',
                        checkerSetId: '',
                        codeLang: '',
                        description: '',
                        catagories: '',
                        baseCheckerSetId: ''
                    }
                    if (this.isEdit) {
                        if (this.isListRouter) this.isCopy = true
                        this.newCheckersetParams = Object.assign(tempForm, this.editObj, { catagories: this.editObj.catagories[0] })
                    } else {
                        this.newCheckersetParams = tempForm
                    }
                    this.getFormParams()
                } else {
                    this.isCopy = false
                }
            },
            'newCheckersetParams.codeLang' (newVal) {
                this.newCheckersetParams.baseCheckerSetId = ''
            },
            'newCheckersetParams.projectId' (newVal) {
                axios
                    .get(`${window.DEVOPS_API_URL}/project/api/user/projects/${newVal}`,
                         { withCredentials: true,
                           headers:
                               { 'X-DEVOPS-PROJECT-ID': newVal }
                         })
                    .then(res => {
                        this.projectName = res.data.data.projectName
                    })
            }
        },
        methods: {
            closeSlider () {
                this.$emit('update:visiable', false)
                this.$emit('update:is-edit', false)
            },
            async submitData () {
                if (!this.hasPermission) return
                this.$refs.checkersetForm.validate().then(validator => {
                    if (validator) {
                        this.isSubmitting = true
                        if (this.isListRouter) {
                            const params = Object.assign({}, this.newCheckersetParams, { catagories: [this.newCheckersetParams.catagories] }, { scope: 0 }, {})
                            if (this.isEdit) params.baseCheckerSetId = this.editObj.baseCheckerSetId
                            this.$store.dispatch('checkerset/create', params).then(res => {
                                if (res.code === '0') {
                                    if (this.isEdit) {
                                        this.refreshDetail()
                                    } else {
                                        const link = {
                                            name: 'checkerset-manage',
                                            params: {
                                                projectId: this.projectId,
                                                checkersetId: this.newCheckersetParams.checkerSetId,
                                                version: 1
                                            }
                                        }
                                        this.$router.push(link)
                                    }
                                    this.closeSlider()
                                }
                            }).finally(() => {
                                this.isSubmitting = false
                            })
                        } else {
                            const { checkersetId } = this.$route.params
                            const { checkerSetName, description, catagories } = this.newCheckersetParams
                            const params = { checkerSetName, description, checkersetId }
                            params.catagories = [catagories]
                            this.$store.dispatch('checkerset/edit', params).then(res => {
                                if (res.code === '0') {
                                    this.refreshDetail()
                                    this.closeSlider()
                                }
                            }).finally(() => {
                                this.isSubmitting = false
                            })
                        }
                    }
                })
            },
            async getFormParams () {
                const res = await this.$store.dispatch('checkerset/params')
                const paramsMap = ['catatories', 'checkerSets', 'codeLangs']
                paramsMap.map(item => {
                    this[item] = res[item]
                })
            }
        }
    }
</script>
<style lang="postcss">
    .create-checkerset-wrapper {
        .bk-sideslider-content {
            height: 100%;
        }
        .slider-content {
            height: auto;
            padding: 30px 60px 30px 30px;
            .pagination {
                padding-top: 10px;
            }
        }
        .checkerset-side-form {
            &.bk-form .bk-form-content {
                line-height: 30px;
            }
        }
    }
    .bk-button.bk-primary.disable-save {
        background-color: #dcdee5;
        border-color: #dcdee5;
        color: #fff;
        cursor: not-allowed;
    }
</style>

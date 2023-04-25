<template>
    <div class="create-meta-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <section class="sub-view-port" v-if="showContent">
            <div class="info-header">
                <div class="title">
                    <i class="devops-icon icon-arrows-left" @click="toMetaDataList()"></i>
                    <span class="header-text">{{title}}</span>
                </div>
                <!-- <a class="job-guide" @click="linkToDocs">脚本任务指标使用指南<i class="devops-icon icon-tiaozhuan"></i></a> -->
            </div>
            <div class="create-meta-content">
                <p class="info-title">{{$t('quality.基本信息')}}</p>
                <hr>
                <bk-form class="create-meta-form" :label-width="100" :model="createForm">
                    <devops-form-item :label="$t('quality.名称')" :required="true" :property="'cnName'"
                        :is-error="errors.has('metaName')"
                        :error-msg="errors.first('metaName')">
                        <bk-input
                            class="meta-name-input"
                            :placeholder="$t('quality.例如：自定义代码覆盖率')"
                            name="metaName"
                            v-model="createForm.cnName"
                            v-validate="{
                                required: true,
                                max: 30
                            }">
                        </bk-input>
                    </devops-form-item>
                    <devops-form-item :label="$t('quality.英文ID')" :required="true" :property="'name'"
                        :is-error="errors.has('metaEnglishName')"
                        :error-msg="errors.first('metaEnglishName')">
                        <bk-input
                            class="meta-name-input"
                            :placeholder="$t('quality.例如：CodeCoverage，创建后不可修改')"
                            name="metaEnglishName"
                            v-model="createForm.name"
                            v-validate="{
                                required: true,
                                max: 30,
                                metaNameRule: true
                            }">
                        </bk-input>
                    </devops-form-item>
                    <bk-form-item :label="$t('quality.描述')" :property="'desc'">
                        <bk-input
                            type="text"
                            class="meta-desc-input"
                            :placeholder="$t('quality.请输入描述')"
                            name="metaDesc"
                            v-model="createForm.desc">
                        </bk-input>
                    </bk-form-item>
                    <devops-form-item :label="$t('quality.数值类型')" :required="true" :property="'dataType'"
                        :is-error="formErrors.typeError"
                        :error-msg="$t('quality.数值类型不能为空')">
                        <bk-select v-model="createForm.dataType" @selected="toggleType">
                            <bk-option v-for="(option, index) in metaTypeList"
                                :key="index"
                                :id="option.id"
                                :name="option.name">
                            </bk-option>
                        </bk-select>
                    </devops-form-item>
                    <devops-form-item :label="$t('quality.可选操作')" :required="true" :property="'operation'"
                        :is-error="formErrors.operationError"
                        :error-msg="$t('quality.可选操作不能为空')">
                        <bk-select multiple v-model="createForm.operation" @selected="toggleOperation">
                            <bk-option v-for="(option, index) in createForm.dataType === 'BOOLEAN' ? boolConf : handleListConf"
                                :key="index"
                                :id="option.id"
                                :name="option.name">
                            </bk-option>
                        </bk-select>
                    </devops-form-item>
                    <devops-form-item :label="$t('quality.默认阈值')" :required="true" :property="'threshold'" class="default-threshlod-item"
                        :is-error="createForm.dataType === 'BOOLEAN' ? formErrors.thresholdError : errors.has('threshold')"
                        :error-msg="errors.first('threshold') || $t('quality.默认阈值不能为空')">
                        <template v-if="createForm.dataType === 'BOOLEAN'">
                            <bk-select v-model="createForm.threshold" @selected="togglethreshold">
                                <bk-option v-for="(option, index) in optionBoolean"
                                    :key="index"
                                    :id="option.value"
                                    :name="option.label">
                                </bk-option>
                            </bk-select>
                        </template>
                        <template v-else-if="createForm.dataType === 'FLOAT'">
                            <bk-input
                                class="meta-threshold-input"
                                name="threshold"
                                v-model="createForm.threshold"
                                v-validate="{
                                    required: true,
                                    floatTypeRule: true
                                }">
                            </bk-input>
                        </template>
                        <template v-else>
                            <bk-input
                                type="number"
                                class="meta-threshold-input"
                                name="threshold"
                                v-model="createForm.threshold"
                                v-validate="{ required: true }">
                            </bk-input>
                        </template>
                    </devops-form-item>
                    <devops-form-item :label="$t('quality.产出插件')" :required="true" :property="'elementType'"
                        :is-error="formErrors.elementTypeError"
                        :error-msg="$t('quality.产出插件不能为空')">
                        <bk-select v-model="createForm.elementType" @selected="formErrors.elementTypeError = false">
                            <bk-option v-for="(option, index) in atomList"
                                :key="index"
                                :id="option.id"
                                :name="option.name">
                            </bk-option>
                        </bk-select>
                    </devops-form-item>
                    <bk-form-item :label="$t('quality.使用说明')">
                        <div class="meta-desc">
                            <img :src="indicatorDescUrl" class="use-nstruction">
                        </div>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button theme="primary" @click.stop.prevent="submitHandle">{{$t('quality.完成')}}</bk-button>
                        <bk-button theme="default" @click="cancelHandle">{{metaId ? $t('quality.删除') : $t('quality.取消')}}</bk-button>
                    </bk-form-item>
                </bk-form>
            </div>
        </section>
    </div>
</template>

<script>
    import i18nImages from '@/utils/i18nImages'
    export default {
        data () {
            return {
                showContent: false,
                isInitEdit: false,
                isEditing: false,
                docsUrl: '',
                title: this.$t('quality.创建脚本任务指标'),
                metaTypeList: [
                    { id: 'INT', name: this.$t('quality.整数（int）') },
                    { id: 'FLOAT', name: this.$t('quality.浮点数（float）') },
                    { id: 'BOOLEAN', name: this.$t('quality.布尔值（bool）') }
                ],
                handleListConf: [
                    { name: '<', id: 'LT' },
                    { name: '<=', id: 'LE' },
                    { name: '=', id: 'EQ' },
                    { name: '>', id: 'GT' },
                    { name: '>=', id: 'GE' }
                ],
                atomList: [
                    { id: 'linuxScript', name: this.$t('quality.脚本任务（linux和macOS环境）') },
                    { id: 'windowsScript', name: this.$t('quality.脚本任务（windows环境）') }
                ],
                boolConf: [
                    { name: '=', id: 'EQ' }
                ],
                optionBoolean: [
                    { label: this.$t('quality.是'), value: 'true' },
                    { label: this.$t('quality.否'), value: 'false' }
                ],
                loading: {
                    isLoading: false,
                    title: ''
                },
                formErrors: {
                    typeError: false,
                    operationError: false,
                    thresholdError: false,
                    elementTypeError: false
                },
                createForm: {
                    name: '',
                    cnName: '',
                    desc: '',
                    dataType: '',
                    threshold: '',
                    elementType: '',
                    operation: []
                },
                metaNameRule: {
                    getMessage: field => this.$t('quality.只能输入英文、数字和下划线'),
                    validate: value => /^[a-zA-Z0-9_]+$/.test(value)
                },
                floatTypeRule: {
                    getMessage: field => this.$t('quality.请输入正确的非负浮点数'),
                    validate: value => /^[0-9]+([.]{1}[0-9]+){0,1}$/.test(value)
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            metaId () {
                return this.$route.params.metaId
            },
            indicatorDescUrl () {
                return i18nImages.indicatorImage[this.$i18n.locale]
            }
        },
        watch: {
            'createForm.dataType' (newVal) {
                if (!this.isInitEdit) {
                    this.createForm.threshold = ''
                    this.createForm.operation = []
                }
            },
            projectId (val) {
                this.$router.push({
                    name: 'qualityOverview',
                    params: {
                        projectId: this.projectId
                    }
                })
            }
        },
        async created () {
            if (this.metaId) {
                this.title = this.$t('quality.编辑脚本任务指标')
                this.isInitEdit = true
                this.isEditing = true
                await this.requestIndicatorDetail()
            } else {
                this.showContent = true
            }
        },
        mounted () {
            this.$nextTick(() => {
                ['metaNameRule', 'floatTypeRule'].map(rule => this.$validator.extend(`${rule}`, this[rule]))
            })
        },
        methods: {
            async requestIndicatorDetail () {
                this.loading.isLoading = true

                try {
                    const res = await this.$store.dispatch('quality/requestIndicatorDetail', {
                        projectId: this.projectId,
                        indicatorId: this.metaId
                    })

                    Object.assign(this.createForm, res, {})
                    this.createForm.name = this.createForm.enName
                    this.createForm.dataType = this.createForm.thresholdType
                    this.createForm.operation = this.createForm.operationList
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                        this.showContent = true
                        this.isInitEdit = false
                    }, 500)
                }
            },
            toMetaDataList () {
                this.$router.push({
                    name: 'metadataList',
                    params: {
                        projectId: this.projectId
                    }
                })
            },
            toggleType () {
                this.formErrors.typeError = false
            },
            toggleOperation (selected) {
                this.formErrors.operationError = selected.length === 0
            },
            togglethreshold () {
                this.formErrors.thresholdError = false
            },
            linkToDocs () {
                window.open(this.docsUrl, '_blank')
            },
            cancelHandle () {
                if (this.metaId) {
                    const h = this.$createElement
                    const content = h('p', {
                        style: {
                            textAlign: 'center'
                        }
                    }, this.$t('quality.确定删除该条指标？'))

                    this.$bkInfo({
                        title: this.$t('quality.删除'),
                        subHeader: content,
                        confirmFn: async () => {
                            this.deleteIndicator()
                        }
                    })
                } else {
                    this.toMetaDataList()
                }
            },
            async deleteIndicator () {
                let message, theme
                this.loading.isLoading = true
                
                try {
                    await this.$store.dispatch('quality/deleteIndicator', {
                        projectId: this.projectId,
                        metaId: this.metaId
                    })

                    message = this.$t('quality.删除成功')
                    theme = 'success'
                } catch (err) {
                    message = err.data ? err.data.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })
                    this.loading.isLoading = false
                    if (theme === 'success') {
                        this.toMetaDataList()
                    }
                }
            },
            checkValid () {
                let errorCount = 0
                const IntReg = /^([0-9]|[1-9][0-9]+)$/ // 自然数
                const floatReg = /^\d+(\.\d+)?$/ // 正浮点数

                if (!this.createForm.dataType) {
                    this.formErrors.typeError = true
                    errorCount++
                }
                if (!this.createForm.operation.length) {
                    this.formErrors.operationError = true
                    errorCount++
                }
                if ((this.createForm.dataType === 'INT' && this.createForm.threshold && !IntReg.test(this.createForm.threshold))
                    || (this.createForm.dataType === 'FLOAT' && this.createForm.threshold && !floatReg.test(this.createForm.threshold))) {
                    this.$bkMessage({
                        message: this.$t('quality.请填写正确的阈值'),
                        theme: 'error'
                    })
                    errorCount++
                }
                if (this.createForm.dataType === 'BOOLEAN' && this.createForm.threshold === '') {
                    this.formErrors.thresholdError = true
                    errorCount++
                }
                if (!this.createForm.elementType) {
                    this.formErrors.elementTypeError = true
                    errorCount++
                }
                if (errorCount > 0) {
                    return false
                }

                return true
            },
            submitHandle () {
                this.$validator.validateAll().then(async (result) => {
                    const isValid = this.checkValid()
                    if (result && isValid) {
                        let message, theme
                        const params = {
                            name: this.createForm.name,
                            cnName: this.createForm.cnName,
                            desc: this.createForm.desc,
                            dataType: this.createForm.dataType,
                            operation: this.createForm.operation,
                            threshold: this.createForm.threshold,
                            elementType: this.createForm.elementType
                        }
                        
                        this.loading.isLoading = true
                        try {
                            if (this.metaId) {
                                await this.$store.dispatch('quality/editIndicator', {
                                    projectId: this.projectId,
                                    indicatorId: this.metaId,
                                    params
                                })

                                message = this.$t('quality.编辑指标成功')
                                theme = 'success'
                            } else {
                                await this.$store.dispatch('quality/createIndicator', {
                                    projectId: this.projectId,
                                    params
                                })

                                message = this.$t('quality.创建指标成功')
                                theme = 'success'
                            }
                        } catch (err) {
                            message = err.message ? err.message : err
                            theme = 'error'
                        } finally {
                            this.loading.isLoading = false
                            this.$bkMessage({
                                message,
                                theme
                            })
                            if (theme === 'success') {
                                this.toMetaDataList()
                            }
                        }
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf.scss';

    .create-meta-wrapper {
        position: relative;
        .sub-view-port {
            height: 100%;
        }
        .info-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid #DDE4EB;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                display: flex;
                align-items: center;
            }
            .header-text {
                font-size: 16px;
            }
            .icon-arrows-left {
                margin-right: 8px;
                cursor: pointer;
                color: $iconPrimaryColor;
                font-size: 16px;
                font-weight: 600;
            }
            .job-guide {
                margin-right: 10px;
                color: $primaryColor;
                cursor: pointer;
            }
            .icon-tiaozhuan {
                position: relative;
                top: 2px;
                margin-left: 8px;
                font-size: 16px;
            }
        }
        .create-meta-content {
            height: calc(100% - 60px);
            padding: 18px 20px;
            overflow: auto;
        }
        .create-meta-form {
            padding-top: 20px;
            .bk-form-input {
                width: 646px;
            }
            .default-threshlod-item .bk-form-input {
                width: 264px;
            }
            .meta-threshold-input {
                width: 264px;
            }
            .bk-select {
                display: inline-block;
                width: 264px;
            }
            .form-tips {
                margin-left: 8px;
                color: #979BA5;
                font-size: 12px;
            }
            .meta-desc {
                width: 646px;
                height: 194px;
                background-color: #C5C7D1;
            }
            .use-nstruction {
                width: 100%;
                height: 100%;
            }
        }
        .info-title {
            color: #737987;
            font-weight: bold;
            .icon-info-circle {
                position: relative;
                top: 2px;
                color: #C3CDD7;
            }
            .title-tips {
                float: right;
                color: $primaryColor;
                font-weight: normal;
                cursor: pointer;
            }
        }
        hr {
            margin-top: 8px;
            height: 1px;
            border: none;
            background-color: #DDE4EB;
        }
        .footer {
            margin: 30px 0 40px 80px;
        }
    }
</style>

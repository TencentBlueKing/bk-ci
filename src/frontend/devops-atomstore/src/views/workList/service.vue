<template>
    <main>
        <div class="content-header">
            <div class="atom-total-row">
                <bk-button theme="primary" @click="relateService"> {{ $t('store.新增微扩展') }} </bk-button>
            </div>
            <bk-input :placeholder="$t('store.请输入关键字搜索')"
                class="search-input"
                :clearable="true"
                :right-icon="'bk-icon icon-search'"
                v-model="searchName">
            </bk-input>
        </div>
        <main class="g-scroll-pagination-table">
            <bk-table style="margin-top: 15px;"
                :empty-text="$t('store.暂时没有微扩展')"
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#fff' }"
                :data="renderList"
                :pagination="pagination"
                @page-change="pageChanged"
                @page-limit-change="pageCountChanged"
                :show-overflow-tooltip="true"
                v-bkloading="{ isLoading }"
            >
                <bk-table-column :label="$t('store.微扩展名称')" width="180">
                    <template slot-scope="props">
                        <span class="atom-name" :title="props.row.serviceName" @click="goToServiceDetail(props.row.serviceCode)">{{ props.row.serviceName }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.微扩展标识')" prop="serviceCode"></bk-table-column>
                <bk-table-column :label="$t('store.调试项目')" prop="projectName"></bk-table-column>
                <bk-table-column :label="$t('store.扩展点')">
                    <template slot-scope="props">
                        <span v-if="props.row.itemName.length <= 0">{{props.row.itemName.length}}</span>
                        <bk-popconfirm v-else trigger="click" ext-cls="custom-popconfirm" title="" confirm-text="" cancel-text="">
                            <div slot="content">
                                <p v-for="(name, index) in props.row.itemName" :key="index" class="service-item">{{ name }}</p>
                            </div>
                            <span class="atom-name">{{props.row.itemName.length}}</span>
                        </bk-popconfirm>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.版本')" prop="version"></bk-table-column>
                <bk-table-column :label="$t('store.状态')">
                    <template slot-scope="props">
                        <status :status="calcStatus(props.row.serviceStatus)"></status>
                        <span>{{ $t(serviceStatusList[props.row.serviceStatus]) }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.修改人')" prop="modifier"></bk-table-column>
                <bk-table-column :label="$t('store.修改时间')" prop="updateTime" width="160"></bk-table-column>
                <bk-table-column :label="$t('store.操作')" width="250" class-name="handler-btn">
                    <template slot-scope="props">
                        <span class="shelf-btn"
                            v-if="props.row.serviceStatus === 'INIT' || props.row.serviceStatus === 'UNDERCARRIAGED'
                                || props.row.serviceStatus === 'GROUNDING_SUSPENSION' || props.row.serviceStatus === 'AUDIT_REJECT'"
                            @click="$router.push({ name: 'editService', params: { serviceId: props.row.serviceId } })"> {{ $t('store.上架') }} </span>
                        <span class="shelf-btn"
                            v-if="props.row.serviceStatus === 'RELEASED'"
                            @click="$router.push({ name: 'editService', params: { serviceId: props.row.serviceId } })"> {{ $t('store.升级') }} </span>
                        <span class="shelf-btn"
                            v-if="props.row.serviceStatus === 'RELEASED' && !props.row.publicFlag"
                            @click="$router.push({ name: 'install', query: { code: props.row.serviceCode, type: 'service', from: 'serviceWork' } })"> {{ $t('store.安装') }} </span>
                        <span class="schedule-btn"
                            v-if="['AUDITING', 'COMMITTING', 'BUILDING', 'EDIT', 'BUILD_FAIL', 'TESTING', 'RELEASE_DEPLOYING', 'RELEASE_DEPLOY_FAIL'].includes(props.row.serviceStatus)"
                            @click="$router.push({ name: 'serviceProgress', params: { serviceId: props.row.serviceId } })"> {{ $t('store.进度') }} </span>
                        <span class="obtained-btn"
                            v-if="props.row.serviceStatus === 'RELEASED' || (props.row.serviceStatus === 'GROUNDING_SUSPENSION' && props.row.releaseFlag)"
                            @click="offline(props.row)"
                        > {{ $t('store.下架') }} </span>
                        <span class="delete-btn"
                            v-if="!props.row.releaseFlag"
                            @click="deleteService(props.row)"
                        > {{ $t('store.删除') }} </span>
                    </template>
                </bk-table-column>
            </bk-table>
        </main>

        <bk-sideslider :is-show.sync="relateServiceData.show"
            @hidden="cancelRelateService"
            :title="relateServiceData.title"
            :quick-close="relateServiceData.quickClose"
            :width="relateServiceData.width">
            <template slot="content">
                <bk-form ref="relateForm"
                    class="relate-form"
                    label-width="100"
                    :model="relateServiceData.form"
                    v-bkloading="{ isLoading: relateServiceData.isLoading }"
                    v-if="hasOauth"
                >
                    <bk-form-item :label="$t('store.微扩展名称')"
                        :required="true"
                        property="serviceName"
                        :desc="$t('store.展示给用户的名称，用户根据名称识别微扩展')"
                        :rules="[requireRule, numMax, nameRule]"
                        error-display-type="normal"
                    >
                        <bk-input v-model="relateServiceData.form.serviceName" :placeholder="$t('store.请输入微扩展名称，不超过20个字符')"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.微扩展标识')"
                        :required="true"
                        property="serviceCode"
                        :desc="$t('store.唯一标识，创建后不能修改。将作为微扩展的代码库名称')"
                        :rules="[requireRule, alpRule, numMax]"
                        error-display-type="normal"
                    >
                        <bk-input v-model="relateServiceData.form.serviceCode" :placeholder="$t('store.请输入微扩展标识，不超过20个字符')"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.扩展点')"
                        :required="true"
                        property="extensionItemList"
                        :desc="$t('store.微扩展服务生效的功能区域')"
                        :rules="[requireRule]"
                        error-display-type="normal"
                    >
                        <bk-select :placeholder="$t('store.请选择扩展点')"
                            :scroll-height="300"
                            :clearable="true"
                            @toggle="getServiceList"
                            :loading="isServiceListLoading"
                            searchable
                            multiple
                            display-tag
                            v-model="relateServiceData.form.extensionItemList">
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
                    <bk-form-item :label="$t('store.调试项目')"
                        required="true"
                        property="projectCode"
                        :desc="{
                            width: 500,
                            content: $t('store.在开发过程中，开发者可在此项目下调试微扩展。成功提交后将不能修改，建议不要选择有正式业务的项目，避免调试过程中影响业务使用'),
                            placement: 'top'
                        }"
                        :rules="[requireRule]"
                        error-display-type="normal"
                    >
                        <bk-select v-model="relateServiceData.form.projectCode"
                            searchable
                            :placeholder="$t('store.请选择项目')"
                            :enable-virtual-scroll="projectList && projectList.length > 3000"
                            :list="projectList"
                            id-key="projectCode"
                            display-key="projectName"
                        >
                            <bk-option
                                v-for="item in projectList"
                                :key="item.projectCode"
                                :id="item.projectCode"
                                :name="item.projectName"
                            >
                            </bk-option>
                            <a href="/console/pm" slot="extension" target="_blank"> {{ $t('store.新增项目') }} </a>
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.开发语言')"
                        :desc="$t('store.后台开发语言')"
                        :required="true"
                        property="language"
                        :rules="[requireRule]"
                        error-display-type="normal"
                    >
                        <bk-select v-model="relateServiceData.form.language" searchable @toggle="getServiceLanguage" :loading="isItemLoading">
                            <bk-option v-for="option in languageList"
                                :key="option"
                                :id="option"
                                :name="option">
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                    <form-tips :tips-content="createTips" class="atom-tip" v-if="!isEnterprise"></form-tips>
                    <bk-form-item>
                        <bk-button theme="primary" @click.native="submitAddService"> {{ $t('store.提交') }} </bk-button>
                        <bk-button @click.native="cancelRelateService"> {{ $t('store.取消') }} </bk-button>
                    </bk-form-item>
                </bk-form>
                <div class="oauth-tips" v-else style="margin: 30px">
                    <button class="bk-button bk-primary" type="button" @click="openValidate"> {{ $t('store.OAUTH认证') }} </button>
                    <p class="prompt-oauth">
                        <i class="devops-icon icon-info-circle-shape"></i>
                        <span> {{ $t('store.新增微扩展时将自动初始化微扩展代码库，请先进行工蜂OAUTH授权') }} </span>
                    </p>
                </div>
            </template>
        </bk-sideslider>

        <bk-sideslider :is-show.sync="offlineServiceData.show"
            :title="offlineServiceData.title"
            :quick-close="offlineServiceData.quickClose"
            :width="offlineServiceData.width">
            <template slot="content">
                <bk-form ref="offlineForm" class="relate-form" label-width="100" :model="offlineServiceData.form" v-bkloading="{ isLoading: offlineServiceData.isLoading }">
                    <bk-form-item :label="$t('store.微扩展名称')" property="serviceName">
                        <span class="lh30">{{offlineServiceData.form.serviceName}}</span>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.微扩展标识')" property="serviceCode">
                        <span class="lh30">{{offlineServiceData.form.serviceCode}}</span>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.下架原因')" :required="true" property="reason" :rules="[requireRule]">
                        <bk-input type="textarea" v-model="offlineServiceData.form.reason" :placeholder="$t('store.请输入下架原因')"></bk-input>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button theme="primary" @click.native="submitOfflineService"> {{ $t('store.提交') }} </bk-button>
                        <bk-button @click.native="cancelOfflineService"> {{ $t('store.取消') }} </bk-button>
                    </bk-form-item>
                </bk-form>
            </template>
        </bk-sideslider>
    </main>
</template>

<script>
    import { debounce } from '@/utils'
    import formTips from '@/components/common/formTips/index'
    import status from './status'
    import { serviceStatusMap } from '@/store/constants'

    export default {
        components: {
            formTips,
            status
        },

        data () {
            return {
                hasOauth: true,
                gitOAuthUrl: '',
                serviceStatusList: serviceStatusMap,
                searchName: '',
                isLoading: false,
                renderList: [],
                projectList: [],
                serviceList: [],
                languageList: [],
                isItemLoading: false,
                isServiceListLoading: false,
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 10
                },
                relateServiceData: {
                    title: this.$t('store.新增微扩展'),
                    quickClose: true,
                    width: 600,
                    isLoading: false,
                    show: false,
                    isLoadingTicketList: false,
                    form: {
                        serviceCode: '',
                        projectCode: '',
                        serviceName: '',
                        extensionItemList: [],
                        language: ''
                    }
                },
                offlineServiceData: {
                    title: this.$t('store.下架微扩展'),
                    quickClose: true,
                    width: 565,
                    isLoading: false,
                    show: false,
                    form: {
                        serviceName: '',
                        serviceCode: '',
                        reason: ''
                    }
                },
                requireRule: {
                    required: true,
                    message: this.$t('store.必填项'),
                    trigger: 'blur'
                },
                numMax: {
                    validator: (val = '') => (val.length <= 20),
                    message: this.$t('store.字段不超过20个字符'),
                    trigger: 'blur'
                },
                alpRule: {
                    validator: (val) => (/^[a-z][a-z0-9-]*(?:[a-z0-9])$/.test(val)),
                    message: this.$t('store.由小写英文字母、数字和中划线组成，且需以小写英文字母开头，以字母或数字结尾'),
                    trigger: 'blur'
                },
                nameRule: {
                    validator: (val) => (/^[\u4e00-\u9fa5a-zA-Z0-9-]*$/.test(val)),
                    message: this.$t('store.由汉字、英文字母、数字、连字符(-)组成，长度小于20个字符'),
                    trigger: 'blur'
                }
            }
        },

        computed: {
            createTips () {
                const host = location.host
                const devHosts = ['dev.devops.woa.com', 'v2.dev.devops.woa.com']
                const testHosts = ['test.devops.woa.com', 'v2.test.devops.woa.com']
                const devIndex = devHosts.findIndex(innerHost => innerHost === host)
                const testIndex = testHosts.findIndex(innerHost => innerHost === host)
                const group = devIndex > -1 ? 'dev-bkdevops-extension-service' : (testIndex > -1 ? 'test-bkdevops-extension-service' : 'bkdevops-extension-service')
                return `${this.$t('store.提交后，系统将在工蜂自动创建代码库，地址示例')}：http://git.woa.com/${group}/${this.relateServiceData.form.serviceCode}.git`
            },

            isEnterprise () {
                return VERSION_TYPE === 'ee'
            }
        },

        watch: {
            searchName () {
                debounce(this.search)
            }
        },

        created () {
            this.requestList()
            this.checkIsOAuth()
        },

        methods: {
            calcStatus (status) {
                let icon = ''
                switch (status) {
                    case 'AUDITING':
                    case 'COMMITTING':
                    case 'BUILDING':
                    case 'EDIT':
                    case 'BUILD_FAIL':
                    case 'UNDERCARRIAGING':
                    case 'TESTING':
                    case 'RELEASE_DEPLOY_FAIL':
                    case 'RELEASE_DEPLOYING':
                        icon = 'doing'
                        break
                    case 'RELEASED':
                        icon = 'success'
                        break
                    case 'GROUNDING_SUSPENSION':
                        icon = 'fail'
                        break
                    case 'AUDIT_REJECT':
                    case 'UNDERCARRIAGED':
                        icon = 'info'
                        break
                    case 'INIT':
                        icon = 'init'
                        break
                }
                return icon
            },

            openValidate () {
                window.open(this.gitOAuthUrl, '_self')
            },

            async checkIsOAuth () {
                try {
                    const res = await this.$store.dispatch('store/checkIsOAuth')
                    this.hasOauth = res.status === 200
                    this.gitOAuthUrl = res.url
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },

            search () {
                this.pagination.current = 1
                this.requestList()
            },

            getServiceLanguage (isOpen) {
                if (!isOpen) return

                this.isItemLoading = true
                this.$store.dispatch('store/requestServiceLanguage').then((res) => {
                    this.languageList = res || []
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isItemLoading = false))
            },

            getServiceList (isOpen) {
                if (!isOpen) return

                this.isServiceListLoading = true
                this.$store.dispatch('store/requestServiceItemList').then((res) => {
                    this.serviceList = (res || []).map((item) => {
                        const serviceItem = item.extServiceItem || {}
                        return {
                            name: serviceItem.name,
                            children: item.childItem || []
                        }
                    })
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isServiceListLoading = false))
            },

            submitOfflineService (row) {
                this.$refs.offlineForm.validate().then(() => {
                    const postData = {
                        serviceCode: this.offlineServiceData.form.serviceCode,
                        params: {
                            version: '',
                            reason: this.offlineServiceData.form.reason
                        }
                    }
                    this.offlineServiceData.isLoading = true
                    this.$store.dispatch('store/requestOfflineService', postData).then((res) => {
                        this.cancelOfflineService()
                        this.requestList()
                    }).catch((err) => {
                        this.$bkMessage({ message: err.message || err, theme: 'error' })
                    }).finally(() => (this.offlineServiceData.isLoading = false))
                }).catch(() => this.$bkMessage({ message: this.$t('store.校验失败，请修改再试'), theme: 'error' }))
            },

            goToServiceDetail (code) {
                this.$router.push({
                    name: 'overView',
                    params: {
                        code,
                        type: 'service'
                    }
                })
            },

            cancelOfflineService () {
                this.offlineServiceData.show = false
                this.offlineServiceData.form.serviceName = ''
                this.offlineServiceData.form.serviceCode = ''
            },

            offline (row) {
                this.offlineServiceData.show = true
                this.offlineServiceData.form.serviceName = row.serviceName
                this.offlineServiceData.form.serviceCode = row.serviceCode
            },

            deleteService (row) {
                const confirmFn = () => {
                    this.isLoading = true
                    this.$store.dispatch('store/requestDelService', row.serviceCode)
                        .then((res) => {
                            this.requestList()
                        }).catch((err) => {
                            this.$bkMessage({ message: err.message || err, theme: 'error' })
                        }).finally(() => (this.isLoading = false))
                }
                this.$bkInfo({
                    title: this.$t('store.确认要删除？'),
                    confirmFn
                })
            },

            submitAddService () {
                this.$refs.relateForm.validate((val) => {
                    if (val) {
                        this.relateServiceData.isLoading = true
                        this.$store.dispatch('store/requestAddService', this.relateServiceData.form).then(() => {
                            this.cancelRelateService()
                            this.requestList()
                        }).catch((err) => {
                            this.$bkMessage({ message: err.message || err, theme: 'error' })
                        }).finally(() => (this.relateServiceData.isLoading = false))
                    }
                }).catch(() => {
                    this.$bkMessage({ message: this.$t('store.校验不通过，请修改后再试'), theme: 'error' })
                })
            },

            cancelRelateService () {
                this.relateServiceData.show = false
                this.relateServiceData.form = {
                    serviceCode: '',
                    projectCode: '',
                    serviceName: '',
                    extensionItemList: [],
                    language: ''
                }
            },

            getProjectList () {
                return this.$store.dispatch('store/requestProjectList').then((res) => {
                    this.projectList = res
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                    this.projectList = []
                })
            },

            requestList () {
                this.isLoading = true
                this.$store.dispatch('store/requestDeskServiceList', { serviceName: this.searchName, page: this.pagination.current, pageSize: this.pagination.limit }).then((res) => {
                    this.renderList = res.records || []
                    this.pagination.count = res.count
                }).catch((err) => {
                    this.renderList = []
                    this.pagination.count = 0
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isLoading = false))
            },

            pageCountChanged (currentLimit, prevLimit) {
                if (currentLimit === this.pagination.limit) return

                this.pagination.current = 1
                this.pagination.limit = currentLimit
                this.requestList()
            },

            pageChanged (page) {
                this.pagination.current = page
                this.requestList()
            },

            relateService () {
                this.relateServiceData.show = true
                this.relateServiceData.isLoading = true
                Promise.all([this.getProjectList()]).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.relateServiceData.isLoading = false))
            }
        }
    }
</script>

<style lang="scss" scoped>
    .select-tag {
        min-height: 30px;
        &::after {
            content: '';
            display: table;
            clear: both;
        }
        li {
            float: left;
            height: 24px;
            background: #f1f2f3;
            border: 1px solid #d9d9d9;
            border-radius: 2px;
            line-height: 24px;
            margin: 3px 5px;
            padding: 0 4px;
            .icon-close {
                margin-right: 3px;
            }
        }
    }
    .service-item {
        line-height: 20px;
    }
    .tag-list {
        padding: 0 20px;
        line-height: 32px;
        font-size: 12px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        color: #63656e;
    }
    .relate-form {
        margin: 30px 20px;
        min-height: 700px;
        .atom-tip {
            margin: 16px 0 16px 21px;
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
</style>

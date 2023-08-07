<template>
    <main>
        <div class="content-header">
            <div class="atom-total-row">
                <bk-button theme="primary" @click="relateImage"> {{ $t('store.关联镜像') }} </bk-button>
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
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#fff' }"
                :data="renderList"
                :pagination="pagination"
                @page-change="pageChanged"
                @page-limit-change="pageCountChanged"
                v-bkloading="{ isLoading }"
            >
                <bk-table-column :label="$t('store.镜像名称')" width="200" show-overflow-tooltip>
                    <template slot-scope="props">
                        <span class="atom-name" :title="props.row.imageName" @click="goToImageDetail(props.row.imageCode)">{{ props.row.imageName }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.镜像来源')" prop="imageSourceType" :formatter="sourceTypeFormatter" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.镜像')" prop="imageRepoUrl" show-overflow-tooltip>
                    <template slot-scope="props">
                        <span :title="(props.row.imageRepoUrl ? props.row.imageRepoUrl + '/' : '') + props.row.imageRepoName + ':' + props.row.imageTag">
                            {{ props.row.imageRepoUrl + props.row.imageRepoName + props.row.imageTag ? (props.row.imageRepoUrl ? props.row.imageRepoUrl + '/' : '') + props.row.imageRepoName + ':' + props.row.imageTag : '-' }}
                        </span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.镜像大小')" prop="imageSize" show-overflow-tooltip>
                    <template slot-scope="props">
                        <span>{{ props.row.imageSize || '-' }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.版本号')" prop="version" show-overflow-tooltip>
                    <template slot-scope="props">
                        <span>{{ props.row.version || '-' }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.状态')" show-overflow-tooltip>
                    <template slot-scope="props">
                        <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary"
                            v-if="['AUDITING', 'COMMITTING', 'CHECKING', 'CHECK_FAIL', 'UNDERCARRIAGING', 'TESTING'].includes(props.row.imageStatus)">
                            <div class="rotate rotate1"></div>
                            <div class="rotate rotate2"></div>
                            <div class="rotate rotate3"></div>
                            <div class="rotate rotate4"></div>
                            <div class="rotate rotate5"></div>
                            <div class="rotate rotate6"></div>
                            <div class="rotate rotate7"></div>
                            <div class="rotate rotate8"></div>
                        </div>
                        <span class="atom-status-icon success" v-if="props.row.imageStatus === 'RELEASED'"></span>
                        <span class="atom-status-icon fail" v-if="props.row.imageStatus === 'GROUNDING_SUSPENSION'"></span>
                        <span class="atom-status-icon obtained" v-if="props.row.imageStatus === 'AUDIT_REJECT' || props.row.imageStatus === 'UNDERCARRIAGED'"></span>
                        <span class="atom-status-icon devops-icon icon-initialize" v-if="props.row.imageStatus === 'INIT'"></span>
                        <span>{{ $t(imageStatusList[props.row.imageStatus]) }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.修改人')" prop="modifier" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.修改时间')" prop="updateTime" width="160" :formatter="timeFormatter" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.操作')" width="250" class-name="handler-btn">
                    <template slot-scope="props">
                        <span class="shelf-btn"
                            v-if="props.row.imageStatus === 'INIT' || props.row.imageStatus === 'UNDERCARRIAGED'
                                || props.row.imageStatus === 'GROUNDING_SUSPENSION' || props.row.imageStatus === 'AUDIT_REJECT'"
                            @click="$router.push({ name: 'editImage', params: { imageId: props.row.imageId } })"> {{ $t('store.上架') }} </span>
                        <span class="shelf-btn"
                            v-if="props.row.imageStatus === 'RELEASED'"
                            @click="$router.push({ name: 'editImage', params: { imageId: props.row.imageId } })"> {{ $t('store.升级') }} </span>
                        <span class="shelf-btn"
                            v-if="props.row.imageStatus === 'RELEASED' && !props.row.publicFlag"
                            @click="$router.push({ name: 'install', query: { code: props.row.imageCode, type: 'image', from: 'imageWork' } })"> {{ $t('store.安装') }} </span>
                        <span class="schedule-btn"
                            v-if="['AUDITING', 'COMMITTING', 'CHECKING', 'CHECK_FAIL', 'UNDERCARRIAGING', 'TESTING'].includes(props.row.imageStatus)"
                            @click="$router.push({ name: 'imageProgress', params: { imageId: props.row.imageId } })"> {{ $t('store.进度') }} </span>
                        <span class="obtained-btn"
                            v-if="props.row.imageStatus === 'RELEASED' || (props.row.imageStatus === 'GROUNDING_SUSPENSION' && props.row.releaseFlag)"
                            @click="offline(props.row)"
                        > {{ $t('store.下架') }} </span>
                        <span @click="deleteImage(props.row.imageCode)" v-if="['INIT', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(props.row.imageStatus)"> {{ $t('store.删除') }} </span>
                    </template>
                </bk-table-column>
                <template #empty>
                    <EmptyTableStatus :type="searchName ? 'search-empty' : 'empty'" @clear="searchName = ''" />
                </template>
            </bk-table>
        </main>

        <bk-sideslider :is-show.sync="relateImageData.show"
            :title="relateImageData.title"
            :quick-close="relateImageData.quickClose"
            :width="relateImageData.width"
            :before-close="cancelRelateImage">
            <template slot="content">
                <bk-form ref="relateForm" class="relate-form" label-width="100" :model="relateImageData.form" v-bkloading="{ isLoading: relateImageData.isLoading }">
                    <bk-form-item :label="$t('store.镜像名称')"
                        :required="true"
                        property="imageName"
                        :desc="$t('store.镜像在研发商店中的别名')"
                        :rules="[requireRule, nameRule]"
                        error-display-type="normal"
                    >
                        <bk-input
                            v-model="relateImageData.form.imageName" :placeholder="$t('store.请输入镜像名称，不超过20个字符')" style="width: 96%;" @change="handleChangeForm"></bk-input>
                        <bk-popover placement="right" class="is-tooltips">
                            <i class="devops-icon icon-info-circle info-icon"></i>
                            <template slot="content">
                                <p> {{ $t('store.由汉字、英文字母、数字、连字符、下划线或点组成，不超过20个字符') }} </p>
                            </template>
                        </bk-popover>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.镜像标识')"
                        :required="true"
                        property="imageCode"
                        :desc="$t('store.镜像英文名，为当前镜像在研发商店中的唯一标识')"
                        :rules="[requireRule, alpRule]"
                        error-display-type="normal"
                    >
                        <bk-input v-model="relateImageData.form.imageCode" :placeholder="$t('store.请输入镜像标识，不超过30个字符')" style="width: 96%;" @change="handleChangeForm"></bk-input>
                        <bk-popover placement="right" class="is-tooltips">
                            <i class="devops-icon icon-info-circle"></i>
                            <template slot="content">
                                <p> {{ $t('store.由英文字母、数字、连字符(-)或下划线(_)组成，以英文字母开头，不超过30个字符') }} </p>
                            </template>
                        </bk-popover>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.镜像源')" :required="true" property="imageSourceType" class="h32" :rules="[requireRule]">
                        <bk-radio-group v-model="relateImageData.form.imageSourceType" @change="handleChangeForm" class="mt6">
                            <bk-radio value="THIRD"> {{ $t('store.第三方源') }} </bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.调试项目')"
                        :required="true"
                        property="projectCode"
                        :desc="$t('store.在发布过程中，可以在该项目下调试镜像')"
                        :rules="[requireRule]"
                        error-display-type="normal"
                    >
                        <bk-select v-model="relateImageData.form.projectCode"
                            @change="toggleProjectList"
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
                    <bk-form-item :label="$t('store.凭证')" property="ticketId" :desc="$t('store.若为私有镜像，请提供凭证，用于流水线执行时拉取镜像')" v-if="relateImageData.form.imageSourceType === 'THIRD'">
                        <bk-select v-model="relateImageData.form.ticketId" searchable :placeholder="$t('store.请选择凭证')" :loading="relateImageData.isLoadingTicketList">
                            <bk-option v-for="option in ticketList"
                                :key="option.credentialId"
                                :id="option.credentialId"
                                :name="option.credentialId">
                            </bk-option>
                            <a v-if="relateImageData.form.projectCode" :href="`/console/ticket/${relateImageData.form.projectCode}/createCredential/USERNAME_PASSWORD/true`" slot="extension" target="_blank"> {{ $t('store.新增凭证') }} </a>
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button theme="primary" @click.native="submitRelateImage"> {{ $t('store.提交') }} </bk-button>
                        <bk-button @click.native="cancelRelateImage"> {{ $t('store.取消') }} </bk-button>
                    </bk-form-item>
                </bk-form>
            </template>
        </bk-sideslider>

        <bk-sideslider :is-show.sync="offlineImageData.show"
            :title="offlineImageData.title"
            :quick-close="offlineImageData.quickClose"
            :width="offlineImageData.width">
            <template slot="content">
                <bk-form ref="offlineForm" class="relate-form" label-width="100" :model="offlineImageData.form" v-bkloading="{ isLoading: offlineImageData.isLoading }">
                    <bk-form-item :label="$t('store.镜像名称')" property="imageName">
                        <span class="lh30">{{offlineImageData.form.imageName}}</span>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.镜像标识')" property="imageCode">
                        <span class="lh30">{{offlineImageData.form.imageCode}}</span>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.镜像版本')" property="version">
                        <bk-select v-model="offlineImageData.form.version" searchable :placeholder="$t('store.请选择镜像版本')">
                            <bk-option v-for="option in offlineImageData.versionList"
                                :key="option.version"
                                :id="option.version"
                                :name="option.version">
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.下架原因')" :required="true" property="reason" :rules="[requireRule]">
                        <bk-input type="textarea" v-model="offlineImageData.form.reason" :placeholder="$t('store.请输入下架原因')"></bk-input>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button theme="primary" @click.native="submitOfflineImage"> {{ $t('store.提交') }} </bk-button>
                        <bk-button @click.native="cancelOfflineImage"> {{ $t('store.取消') }} </bk-button>
                    </bk-form-item>
                </bk-form>
            </template>
        </bk-sideslider>
    </main>
</template>

<script>
    import { debounce } from '@/utils/index'
    import { imageStatusList } from '@/store/constants'

    export default {
        data () {
            return {
                imageStatusList,
                searchName: '',
                isLoading: false,
                renderList: [],
                projectList: [],
                ticketList: [],
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 10
                },
                relateImageData: {
                    title: this.$t('store.关联镜像'),
                    quickClose: true,
                    width: 565,
                    isLoading: false,
                    show: false,
                    isLoadingTicketList: false,
                    form: {
                        imageCode: '',
                        projectCode: '',
                        imageName: '',
                        imageSourceType: 'THIRD',
                        ticketId: ''
                    }
                },
                offlineImageData: {
                    title: this.$t('store.下架镜像'),
                    quickClose: true,
                    width: 565,
                    isLoading: false,
                    show: false,
                    versionList: [],
                    form: {
                        imageName: '',
                        imageCode: '',
                        version: '',
                        reason: ''
                    }
                },
                requireRule: {
                    required: true,
                    message: this.$t('store.必填项'),
                    trigger: 'blur'
                },
                alpRule: {
                    validator: (val) => (/^[a-zA-Z][a-zA-Z0-9-_]{1,30}$/.test(val)),
                    message: this.$t('store.由英文字母、数字、连字符(-)或下划线(_)组成，以英文字母开头，不超过30个字符'),
                    trigger: 'blur'
                },
                nameRule: {
                    validator: (val) => (/^[\u4e00-\u9fa5a-zA-Z0-9-_.]{1,20}$/.test(val)),
                    message: this.$t('store.由汉字、英文字母、数字、连字符、下划线或点组成，不超过20个字符'),
                    trigger: 'blur'
                }
            }
        },

        watch: {
            searchName () {
                this.isLoading = true
                debounce(this.search)
            }
        },

        created () {
            this.requestList()
            this.getProjectList()
        },

        methods: {
            search () {
                this.pagination.current = 1
                this.requestList()
            },

            submitOfflineImage (row) {
                this.$refs.offlineForm.validate().then(() => {
                    const postData = {
                        imageCode: this.offlineImageData.form.imageCode,
                        params: {
                            version: this.offlineImageData.form.version,
                            reason: this.offlineImageData.form.reason
                        }
                    }
                    this.offlineImageData.isLoading = true
                    this.$store.dispatch('store/requestOfflineImage', postData).then((res) => {
                        this.cancelOfflineImage()
                        this.requestList()
                    }).catch((err) => {
                        this.$bkMessage({ message: err.message || err, theme: 'error' })
                    }).finally(() => (this.offlineImageData.isLoading = false))
                }).catch(() => this.$bkMessage({ message: this.$t('store.校验失败，请修改再试'), theme: 'error' }))
            },

            goToImageDetail (code) {
                this.$router.push({
                    name: 'version',
                    params: {
                        code,
                        type: 'image'
                    }
                })
            },

            cancelOfflineImage () {
                this.offlineImageData.show = false
                this.offlineImageData.form.imageName = ''
                this.offlineImageData.form.imageCode = ''
                this.offlineImageData.form.version = ''
            },

            offline (row) {
                this.offlineImageData.show = true
                this.offlineImageData.form.imageName = row.imageName
                this.offlineImageData.form.imageCode = row.imageCode
                this.offlineImageData.isLoading = true

                const postData = {
                    imageCode: row.imageCode,
                    page: 1,
                    pageSize: 1000
                }
                this.offlineImageData.isLoading = true
                this.$store.dispatch('store/requestImageVersionList', postData).then((res) => {
                    this.offlineImageData.versionList = (res.records || []).filter((image) => {
                        return image.imageStatus === 'RELEASED' || (image.imageStatus === 'GROUNDING_SUSPENSION' && image.releaseFlag)
                    })
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.offlineImageData.isLoading = false))
            },

            deleteImage (imageCode) {
                const confirmFn = () => {
                    this.isLoading = true
                    this.$store.dispatch('store/requestDelImage', imageCode).then((res) => {
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

            submitRelateImage () {
                this.$refs.relateForm.validate((val) => {
                    if (val) {
                        const postData = {
                            imageCode: this.relateImageData.form.imageCode,
                            params: this.relateImageData.form
                        }
                        this.relateImageData.isLoading = true
                        this.$store.dispatch('store/requestRelImage', postData).then(() => {
                            this.relateImageData.form = {
                                projectCode: '',
                                imageName: '',
                                imageSourceType: 'BKDEVOPS',
                                ticketId: ''
                            }
                            setTimeout(() => {
                                this.relateImageData.show = false
                            })
                            this.requestList()
                        }).catch((err) => {
                            this.$bkMessage({ message: err.message || err, theme: 'error' })
                        }).finally(() => (this.relateImageData.isLoading = false))
                    }
                }).catch(() => {
                    this.$bkMessage({ message: this.$t('store.校验不通过，请修改后再试'), theme: 'error' })
                })
            },

            cancelRelateImage () {
                if (window.changeFlag) {
                    this.$bkInfo({
                        title: this.$t('确认离开当前页？'),
                        subHeader: this.$createElement('p', {
                            style: {
                                color: '#63656e',
                                fontSize: '14px',
                                textAlign: 'center'
                            }
                        }, this.$t('离开将会导致未保存信息丢失')),
                        okText: this.$t('离开'),
                        confirmFn: () => {
                            this.relateImageData.form = {
                                projectCode: '',
                                imageName: '',
                                imageSourceType: 'BKDEVOPS',
                                ticketId: ''
                            }
                            setTimeout(() => {
                                this.relateImageData.show = false
                            })
                            return true
                        }
                    })
                } else {
                    this.relateImageData.show = false
                    this.relateImageData.form = {
                        projectCode: '',
                        imageName: '',
                        imageSourceType: 'BKDEVOPS',
                        ticketId: ''
                    }
                }
            },

            handleChangeForm () {
                window.changeFlag = true
            },
            toggleProjectList () {
                this.handleChangeForm()
                this.relateImageData.form.ticketId = ''
                const projectCode = this.relateImageData.form.projectCode
                if (!projectCode) return
                this.relateImageData.isLoadingTicketList = true
                this.$store.dispatch('store/requestTicketList', { projectCode }).then((res) => {
                    this.ticketList = res.records || []
                }).catch((err) => {
                    this.ticketList = []
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.relateImageData.isLoadingTicketList = false))
            },

            getProjectList () {
                this.$store.dispatch('store/requestProjectList').then((res) => {
                    this.projectList = res
                }).catch(() => {
                    this.projectList = []
                })
            },

            requestList () {
                this.isLoading = true
                this.$store.dispatch('store/requestDeskImageList', { imageName: this.searchName, page: this.pagination.current, pageSize: this.pagination.limit }).then((res) => {
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

            relateImage () {
                window.changeFlag = false
                this.relateImageData.show = true
            },

            timeFormatter (row, column, cellValue, index) {
                const date = new Date(cellValue)
                const year = date.toISOString().slice(0, 10)
                const time = date.toTimeString().split(' ')[0]
                return `${year} ${time}`
            },

            sourceTypeFormatter (row, column, cellValue, index) {
                let res = ''
                switch (cellValue) {
                    case 'BKDEVOPS':
                        res = this.$t('store.蓝盾')
                        break
                    default:
                        res = this.$t('store.第三方')
                        break
                }
                return res
            }
        }
    }
</script>

<style lang="scss" scope>
    .relate-form {
        margin: 30px 20px;
        min-height: 700px;
        position: relative;
        .is-tooltips {
            position: absolute;
            right: -5px;
            top: 3px;
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

<template>
    <main>
        <div class="content-header">
            <div class="atom-total-row">
                <button class="bk-button bk-primary" @click="relateImage">
                    <span style="margin-left: 0;"> {{ $t('关联镜像') }} </span>
                </button>
            </div>
            <section :class="[{ 'control-active': isInputFocus }, 'g-input-search', 'list-input']">
                <input class="g-input-border" type="text" :placeholder="$t('请输入关键字搜索')" v-model="searchName" @focus="isInputFocus = true" @blur="isInputFocus = false" @keyup.enter="search" />
                <i class="bk-icon icon-search" v-if="!searchName"></i>
                <i class="bk-icon icon-close-circle-shape clear-icon" v-else @click="clearSearch"></i>
            </section>
        </div>
        <bk-table style="margin-top: 15px;" :empty-text="$t('暂时没有镜像')"
            :data="renderList"
            :pagination="pagination"
            @page-change="pageChanged"
            @page-limit-change="pageCountChanged"
            v-bkloading="{ isLoading }"
        >
            <bk-table-column :label="$t('镜像名称')" width="200">
                <template slot-scope="props">
                    <span class="atom-name" :title="props.row.imageName" @click="goToImageDetail(props.row.imageCode)">{{ props.row.imageName }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('镜像来源')" prop="imageSourceType" :formatter="sourceTypeFormatter" width="150"></bk-table-column>
            <bk-table-column :label="$t('镜像')" prop="imageRepoUrl">
                <template slot-scope="props">
                    <span :title="(props.row.imageRepoUrl ? props.row.imageRepoUrl + '/' : '') + props.row.imageRepoName + ':' + props.row.imageTag">
                        {{ props.row.imageRepoUrl + props.row.imageRepoName + props.row.imageTag ? (props.row.imageRepoUrl ? props.row.imageRepoUrl + '/' : '') + props.row.imageRepoName + ':' + props.row.imageTag : '-' }}
                    </span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('镜像大小')" prop="imageSize" width="150">
                <template slot-scope="props">
                    <span>{{ props.row.imageSize || '-' }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('版本号')" prop="version" width="150">
                <template slot-scope="props">
                    <span>{{ props.row.version || '-' }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('状态')" width="220">
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
                    <span class="atom-status-icon bk-icon icon-initialize" v-if="props.row.imageStatus === 'INIT'"></span>
                    <span>{{ $t(imageStatusList[props.row.imageStatus]) }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('修改人')" prop="modifier" width="180"></bk-table-column>
            <bk-table-column :label="$t('修改时间')" prop="updateTime" width="180" :formatter="timeFormatter"></bk-table-column>
            <bk-table-column :label="$t('操作')" width="250" class-name="handler-btn">
                <template slot-scope="props">
                    <span class="shelf-btn"
                        v-if="props.row.imageStatus === 'INIT' || props.row.imageStatus === 'UNDERCARRIAGED'
                            || props.row.imageStatus === 'GROUNDING_SUSPENSION' || props.row.imageStatus === 'AUDIT_REJECT'"
                        @click="$router.push({ name: 'editImage', params: { imageId: props.row.imageId } })"> {{ $t('上架') }} </span>
                    <span class="shelf-btn"
                        v-if="props.row.imageStatus === 'RELEASED'"
                        @click="$router.push({ name: 'editImage', params: { imageId: props.row.imageId } })"> {{ $t('升级') }} </span>
                    <span class="shelf-btn"
                        v-if="props.row.imageStatus === 'RELEASED'"
                        @click="$router.push({ name: 'install', query: { code: props.row.imageCode, type: 'image', from: 'atomList' } })"> {{ $t('安装') }} </span>
                    <span class="schedule-btn"
                        v-if="['AUDITING', 'COMMITTING', 'CHECKING', 'CHECK_FAIL', 'UNDERCARRIAGING', 'TESTING'].includes(props.row.imageStatus)"
                        @click="$router.push({ name: 'imageProgress', params: { imageId: props.row.imageId } })"> {{ $t('进度') }} </span>
                    <span class="obtained-btn"
                        v-if="props.row.imageStatus === 'RELEASED' || (props.row.imageStatus === 'GROUNDING_SUSPENSION' && props.row.releaseFlag)"
                        @click="offline(props.row)"
                    > {{ $t('下架') }} </span>
                    <span @click="deleteImage(props.row.imageCode)" v-if="['INIT', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(props.row.imageStatus)"> {{ $t('删除') }} </span>
                </template>
            </bk-table-column>
        </bk-table>

        <bk-sideslider :is-show.sync="relateImageData.show"
            :title="relateImageData.title"
            :quick-close="relateImageData.quickClose"
            :width="relateImageData.width">
            <template slot="content">
                <bk-form ref="relateForm" class="relate-form" label-width="100" :model="relateImageData.form" v-bkloading="{ isLoading: relateImageData.isLoading }">
                    <bk-form-item :label="$t('镜像名称')" :required="true" property="imageName" :desc="$t('镜像在研发商店中的别名')" :rules="[requireRule]">
                        <bk-input v-model="relateImageData.form.imageName" :placeholder="$t('请输入镜像名称')"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('镜像标识')" :required="true" property="imageCode" :desc="$t('镜像在研发商店中的唯一标识')" :rules="[requireRule, alpRule]">
                        <bk-input v-model="relateImageData.form.imageCode" :placeholder="$t('请输入镜像标识')"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('镜像源')" :required="true" property="imageSourceType" class="h32" :rules="[requireRule]">
                        <bk-radio-group v-model="relateImageData.form.imageSourceType" class="mt6">
                            <bk-radio value="BKDEVOPS" class="mr12"> {{ $t('蓝盾源') }} </bk-radio>
                            <bk-radio value="THIRD"> {{ $t('第三方源') }} </bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                    <bk-form-item :label="$t('调试项目')" :required="true" property="projectCode" :desc="$t('在发布过程中，可以在该项目下调试镜像')" :rules="[requireRule]">
                        <bk-select v-model="relateImageData.form.projectCode" searchable :placeholder="$t('请选择项目')" @change="toggleProjectList">
                            <bk-option v-for="option in projectList"
                                :key="option.project_code"
                                :id="option.project_code"
                                :name="option.project_name">
                            </bk-option>
                            <a href="/console/pm" slot="extension" target="_blank"> {{ $t('新增项目') }} </a>
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item :label="$t('凭证')" property="ticketId" :desc="$t('若为私有镜像，请提供凭证，用于流水线执行时拉取镜像')" v-if="relateImageData.form.imageSourceType === 'THIRD'">
                        <bk-select v-model="relateImageData.form.ticketId" searchable :placeholder="$t('请选择凭证')" :loading="relateImageData.isLoadingTicketList">
                            <bk-option v-for="option in ticketList"
                                :key="option.credentialId"
                                :id="option.credentialId"
                                :name="option.credentialId">
                            </bk-option>
                            <a v-if="relateImageData.form.projectCode" :href="`/console/ticket/${relateImageData.form.projectCode}/createCredential/USERNAME_PASSWORD/true`" slot="extension" target="_blank"> {{ $t('新增凭证') }} </a>
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button theme="primary" @click.native="submitRelateImage"> {{ $t('提交') }} </bk-button>
                        <bk-button @click.native="cancelRelateImage"> {{ $t('取消') }} </bk-button>
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
                    <bk-form-item :label="$t('镜像名称')" property="imageName">
                        <span class="lh30">{{offlineImageData.form.imageName}}</span>
                    </bk-form-item>
                    <bk-form-item :label="$t('镜像标识')" property="imageCode">
                        <span class="lh30">{{offlineImageData.form.imageCode}}</span>
                    </bk-form-item>
                    <bk-form-item :label="$t('镜像版本')" property="version">
                        <bk-select v-model="offlineImageData.form.version" searchable :placeholder="$t('请选择镜像版本')">
                            <bk-option v-for="option in offlineImageData.versionList"
                                :key="option.version"
                                :id="option.version"
                                :name="option.version">
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item :label="$t('下架原因')" :required="true" property="reason" :rules="[requireRule]">
                        <bk-input type="textarea" v-model="offlineImageData.form.reason" :placeholder="$t('请输入下架原因')"></bk-input>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button theme="primary" @click.native="submitOfflineImage"> {{ $t('提交') }} </bk-button>
                        <bk-button @click.native="cancelOfflineImage"> {{ $t('取消') }} </bk-button>
                    </bk-form-item>
                </bk-form>
            </template>
        </bk-sideslider>
    </main>
</template>

<script>
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
                    title: this.$t('关联镜像'),
                    quickClose: true,
                    width: 565,
                    isLoading: false,
                    show: false,
                    isLoadingTicketList: false,
                    form: {
                        imageCode: '',
                        projectCode: '',
                        imageName: '',
                        imageSourceType: 'BKDEVOPS',
                        ticketId: ''
                    }
                },
                offlineImageData: {
                    title: this.$t('下架镜像'),
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
                    message: this.$t('必填项'),
                    trigger: 'blur'
                },
                alpRule: {
                    validator: (val) => (/^[a-zA-Z0-9-_]+$/.test(val)),
                    message: this.$t('标识需要是大小写字母、数字、中划线或下划线'),
                    trigger: 'blur'
                }
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
                }).catch(() => this.$bkMessage({ message: this.$t('校验失败，请修改再试'), theme: 'error' }))
            },

            goToImageDetail (imageCode) {
                this.$router.push({
                    name: 'imageOverview',
                    params: {
                        imageCode
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
                this.offlineImageData.form.version = row.version
                this.offlineImageData.isLoading = true

                const postData = {
                    imageCode: row.imageCode,
                    page: 1,
                    pageSize: 1000
                }
                this.offlineImageData.isLoading = true
                this.$store.dispatch('store/requestImageVersionList', postData).then((res) => {
                    this.offlineImageData.versionList = res.records || []
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
                    title: this.$t('确认要删除？'),
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
                            this.cancelRelateImage()
                            this.requestList()
                        }).catch((err) => {
                            this.$bkMessage({ message: err.message || err, theme: 'error' })
                        }).finally(() => (this.relateImageData.isLoading = false))
                    }
                }).catch(() => {
                    this.$bkMessage({ message: this.$t('校验不通过，请修改后再试'), theme: 'error' })
                })
            },

            cancelRelateImage () {
                this.relateImageData.show = false
                this.relateImageData.form = {
                    projectCode: '',
                    imageName: '',
                    imageSourceType: 'BKDEVOPS',
                    ticketId: ''
                }
            },

            toggleProjectList () {
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

            clearSearch () {
                this.searchName = ''
                this.requestList()
            },

            relateImage () {
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
                        res = this.$t('蓝盾')
                        break
                    default:
                        res = this.$t('第三方')
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

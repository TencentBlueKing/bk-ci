<template>
    <article class="image-detail-home" v-bkloading="{ isLoading }">
        <div class="inner-header">
            <div class="title"> {{ $t('store.镜像详情') }} </div>
            <span @click="goToEditImage" :class="[{ 'disable': !showEdit }, 'header-edit']" :title="!showEdit && $t('store.只有处于审核驳回、已发布、上架中止和已下架的状态才允许修改基本信息')"> {{ $t('store.编辑') }} </span>
        </div>
        <main class="detail-main">
            <detail-info :detail="currentImage"></detail-info>
            <section class="version-content" v-if="!isLoading">
                <div class="version-info-header">
                    <div class="info-title"> {{ $t('store.版本列表') }} </div>
                    <button class="bk-button bk-primary"
                        type="button"
                        :disabled="upgradeStatus.indexOf((versionList[0] || {}).imageStatus) === -1"
                        @click="$router.push({ name: 'editImage', params: { imageId: versionList[0].imageId } })"
                    > {{ $t('store.新增版本') }} </button>
                </div>
                <bk-table style="margin-top: 15px;"
                    :data="versionList"
                    :pagination="pagination"
                    @page-change="pageChanged"
                    @page-limit-change="pageCountChanged"
                >
                    <bk-table-column :label="$t('store.版本')" prop="version"></bk-table-column>
                    <bk-table-column :label="$t('store.状态')" prop="imageStatus" :formatter="statusFormatter"></bk-table-column>
                    <bk-table-column :label="$t('store.创建人')" prop="creator"></bk-table-column>
                    <bk-table-column :label="$t('store.创建时间')" prop="createTime" :formatter="convertTime"></bk-table-column>
                    <bk-table-column :label="$t('store.操作')" width="120" class-name="handler-btn">
                        <template slot-scope="props">
                            <section v-show="!index">
                                <span class="update-btn"
                                    v-if="props.row.imageStatus === 'INIT'"
                                    @click="$router.push({ name: 'editImage', params: { imageId: props.row.imageId } })"
                                > {{ $t('store.上架') }} </span>
                                <span class="update-btn"
                                    v-if="progressStatus.indexOf(props.row.imageStatus) > -1"
                                    @click="$router.push({ name: 'imageProgress', params: { imageId: props.row.imageId } })"
                                > {{ $t('store.进度') }} </span>
                                <span class="obtained-btn"
                                    v-if="props.row.imageStatus === 'RELEASED' || (props.row.imageStatus === 'GROUNDING_SUSPENSION' && props.row.releaseFlag)"
                                    @click="offline(props.row)"
                                > {{ $t('store.下架') }} </span>
                            </section>
                        </template>
                    </bk-table-column>
                </bk-table>
            </section>

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
                            <span class="lh30">{{offlineImageData.form.version}}</span>
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
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import { imageStatusList } from '@/store/constants'
    import { convertTime } from '../../utils/index'
    import detailInfo from '@/components/detailInfo'

    export default {
        components: {
            detailInfo
        },

        data () {
            return {
                isLoading: false,
                showEdit: false,
                versionList: [],
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 10
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
                progressStatus: ['COMMITTING', 'CHECKING', 'CHECK_FAIL', 'TESTING', 'AUDITING'],
                upgradeStatus: ['AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION']
            }
        },

        computed: {
            ...mapGetters('store', {
                'currentImage': 'getCurrentImage'
            })
        },

        created () {
            this.getVersionList()
        },

        methods: {
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
                        this.getVersionList()
                    }).catch((err) => {
                        this.$bkMessage({ message: err.message || err, theme: 'error' })
                    }).finally(() => (this.offlineImageData.isLoading = false))
                }).catch(() => this.$bkMessage({ message: this.$t('store.校验失败，请修改再试'), theme: 'error' }))
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
            },

            getVersionList () {
                const postData = {
                    imageCode: this.currentImage.imageCode,
                    page: this.pagination.current,
                    pageSize: this.pagination.limit
                }
                this.isLoading = true
                this.$store.dispatch('store/requestImageVersionList', postData).then((res) => {
                    this.versionList = res.records || []
                    this.pagination.count = res.count
                    const lastestVersion = this.versionList[0] || {}
                    const lastestStatus = lastestVersion.imageStatus
                    this.showEdit = ['AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(lastestStatus)
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => (this.isLoading = false))
            },

            goToEditImage () {
                if (!this.showEdit) return
                this.$router.push({ name: 'imageEdit' })
            },

            pageCountChanged (currentLimit, prevLimit) {
                if (currentLimit === this.pagination.limit) return

                this.pagination.current = 1
                this.pagination.limit = currentLimit
                this.getVersionList()
            },

            pageChanged (page) {
                this.pagination.current = page
                this.getVersionList()
            },

            statusFormatter (row, column, cellValue, index) {
                return this.$t(imageStatusList[cellValue])
            },

            convertTime (row, column, cellValue, index) {
                return convertTime(cellValue)
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';
    .image-detail-home {
        height: 100%;
        overflow: hidden;
        .detail-main {
            height: calc(100% - 60px);
            width: calc(100vw - 261px);
            overflow: hidden auto;
            padding: 20px 0;
        }
    }

    .inner-header {
        display: flex;
        justify-content: space-between;
        padding: 0 20px;
        width: 100%;
        height: 60px;
        border-bottom: 1px solid $borderWeightColor;
        background-color: #fff;
        box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
        .title {
            font-size: 16px;
            line-height: 59px;
        }
        .header-edit {
            font-size: 16px;
            line-height: 59px;
            color: $primaryColor;
            cursor: pointer;
        }
    }

    .version-content {
        padding: 0 20px;
    }

    .version-info-header {
        display: flex;
        justify-content: space-between;
        margin-top: 36px;
        .info-title {
            font-weight: bold;
            line-height: 2.5;
        }
    }
    .version-table {
        margin-top: 10px;
        border: 1px solid $borderWeightColor;
        tbody {
            background-color: #fff;
        }
        th {
            height: 42px;
            padding: 2px 10px;
            color: #333C48;
            font-weight: normal;
            &:first-child {
                padding-left: 20px;
            }
        }
        td {
            height: 42px;
            padding: 2px 10px;
            &:first-child {
                padding-left: 20px;
            }
            &:last-child {
                padding-right: 30px;
            }
        }
        .handler-btn {
            span {
                display: inline-block;
                margin-right: 20px;
                color: $primaryColor;
                cursor: pointer;
            }
        }
        .no-data-row {
            padding: 40px;
            color: $fontColor;
            text-align: center;
        }
    }
</style>

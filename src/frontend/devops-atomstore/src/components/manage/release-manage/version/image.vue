<template>
    <section class="show-version g-scroll-pagination-table">
        <bk-button theme="primary"
            class="version-button"
            :disabled="disableAddVersion"
            @click="$router.push({
                name: 'editImage',
                params: {
                    imageId: versionList[0].imageId
                }
            })"
        > {{ $t('store.新增版本') }} </bk-button>
        <bk-table :data="versionList"
            :outer-border="false"
            :header-border="false"
            :header-cell-style="{ background: '#fff' }"
            :pagination="pagination"
            @page-change="(page) => $emit('pageChanged', page)"
            @page-limit-change="(currentLimit, prevLimit) => $emit('pageLimitChanged', currentLimit, prevLimit)"
        >
            <bk-table-column :label="$t('store.版本')" show-overflow-tooltip>
                <template slot-scope="props">
                    <span>{{ props.row.version || 'init' }}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('store.镜像Tag')" prop="imageTag" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.状态')" prop="imageStatus" :formatter="statusFormatter" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.创建人')" prop="creator" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.创建时间')" prop="createTime" :formatter="convertTime" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.操作')" width="150" class-name="handler-btn">
                <template slot-scope="props">
                    <section v-show="!index">
                        <span class="update-btn" @click="showDetail(props.row.imageId)">{{ $t('store.查看') }}</span>
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
                    <bk-form-item :label="$t('store.下架原因')" :required="true" property="reason" :rules="[requireRule($t('store.下架原因'))]">
                        <bk-input type="textarea" v-model="offlineImageData.form.reason" :placeholder="$t('store.请输入下架原因')"></bk-input>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button theme="primary" @click.native="submitOfflineImage"> {{ $t('store.提交') }} </bk-button>
                        <bk-button @click.native="cancelOfflineImage"> {{ $t('store.取消') }} </bk-button>
                    </bk-form-item>
                </bk-form>
            </template>
        </bk-sideslider>

        <bk-sideslider quick-close
            class="offline-atom-slider"
            :is-show.sync="hasShowDetail"
            :title="$t('store.查看详情')"
            :width="800">
            <template slot="content">
                <image-detail :detail="detail" v-bkloading="{ isLoading: detailLoading }" class="version-detail"></image-detail>
            </template>
        </bk-sideslider>
    </section>
</template>

<script>
    import { imageStatusList } from '@/store/constants'
    import { convertTime } from '@/utils/index'
    import imageDetail from '../../detail/image-detail/show.vue'

    export default {
        components: {
            imageDetail
        },

        props: {
            versionList: Array,
            pagination: Object
        },

        data () {
            return {
                progressStatus: ['COMMITTING', 'CHECKING', 'CHECK_FAIL', 'TESTING', 'AUDITING'],
                upgradeStatus: ['INIT', 'UNDERCARRIAGED', 'AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION'],
                isOverDes: false,
                hasShowAll: false,
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
                hasShowDetail: false,
                detailLoading: false,
                detail: {}
            }
        },

        computed: {
            disableAddVersion () {
                const firstVersion = this.versionList[0] || {}
                return this.upgradeStatus.indexOf(firstVersion.imageStatus) === -1
            }
        },

        methods: {
            showDetail (imageId) {
                this.hasShowDetail = true
                this.detailLoading = true
                this.$store.dispatch('store/requestImageDetail', imageId).then((res) => {
                    this.detail = res || {}
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.detailLoading = false
                })
            },

            requireRule (name) {
                return {
                    required: true,
                    message: this.$t('store.validateMessage', [name, this.$t('store.必填项')]),
                    trigger: 'blur'
                }
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
                        this.$emit('pageChanged')
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
    .version-detail {
        padding: 20px;
    }
</style>

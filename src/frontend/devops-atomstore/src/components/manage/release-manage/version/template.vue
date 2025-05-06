<template>
    <section class="show-version g-scroll-pagination-table">
        <bk-button
            theme="primary"
            class="version-button"
        >
            {{ $t('store.新增版本') }}
        </bk-button>
        <bk-table
            :data="versionList"
            :outer-border="false"
            :header-border="false"
            :header-cell-style="{ background: '#fff' }"
            :pagination="pagination"
            @page-change="handlePageChange"
            @page-limit-change="handleLimitChange"
        >
            <bk-table-column
                v-for="col in columns"
                :key="col.prop"
                v-bind="col"
            />
            <bk-table-column
                :label="$t('store.操作')"
                class-name="handler-btn"
            >
                <template slot-scope="props">
                    <section v-show="!index">
                        <bk-button
                            text
                            theme="primary"
                            size="small"
                            @click="toDetail"
                        >
                            {{ $t('store.查看') }}
                        </bk-button>
                        <bk-button
                            text
                            theme="primary"
                            size="small"
                            v-if="props.row.templateStatus === 'RELEASED' || (props.row.templateStatus === 'GROUNDING_SUSPENSION' && props.row.releaseFlag)"
                            @click="offline(props.row)"
                        >
                            {{ $t('store.下架') }}
                        </bk-button>
                    </section>
                </template>
            </bk-table-column>
        </bk-table>

        <bk-sideslider
            :is-show.sync="offlineImageData.show"
            :title="offlineImageData.title"
            :quick-close="offlineImageData.quickClose"
            :width="offlineImageData.width"
        >
            <template slot="content">
                <bk-form
                    ref="offlineForm"
                    class="relate-form"
                    label-width="100"
                    :model="offlineImageData.form"
                    v-bkloading="{ isLoading: offlineImageData.isLoading }"
                >
                    <bk-form-item
                        :label="$t('store.镜像名称')"
                        property="imageName"
                    >
                        <span class="lh30">{{ offlineImageData.form.imageName }}</span>
                    </bk-form-item>
                    <bk-form-item
                        :label="$t('store.镜像标识')"
                        property="imageCode"
                    >
                        <span class="lh30">{{ offlineImageData.form.imageCode }}</span>
                    </bk-form-item>
                    <bk-form-item
                        :label="$t('store.镜像版本')"
                        property="version"
                    >
                        <span class="lh30">{{ offlineImageData.form.version }}</span>
                    </bk-form-item>
                    <bk-form-item
                        :label="$t('store.下架原因')"
                        :required="true"
                        property="reason"
                        :rules="[requireRule($t('store.下架原因'))]"
                    >
                        <bk-input
                            type="textarea"
                            v-model="offlineImageData.form.reason"
                            :placeholder="$t('store.请输入下架原因')"
                        ></bk-input>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button
                            theme="primary"
                            @click.native="submitOfflineImage"
                        >
                            {{ $t('store.提交') }}
                        </bk-button>
                        <bk-button @click.native="cancelOfflineImage"> {{ $t('store.取消') }} </bk-button>
                    </bk-form-item>
                </bk-form>
            </template>
        </bk-sideslider>
    </section>
</template>

<script>
    import { templateStatusList } from '@/store/constants'
    import { convertTime } from '@/utils/index'

    export default {

        props: {
            versionList: Array,
            pagination: Object
        },

        data () {
            return {
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
            columns () {
                return [
                    {
                        label: this.$t('store.版本'),
                        prop: 'version'
                    },
                    {
                        label: this.$t('store.状态'),
                        prop: 'templateStatus',
                        formatter: this.statusFormatter
                    },
                    {
                        label: this.$t('store.创建人'),
                        prop: 'creator'
                    },
                    {
                        label: this.$t('store.创建时间'),
                        prop: 'createTime',
                        formatter: this.convertTime
                    }
                ]
            }
        },

        methods: {
            handlePageChange (page) {
                this.$emit('pageChanged', page)
            },
            handleLimitChange (currentLimit, prevLimit) {
                this.$emit('pageLimitChanged', currentLimit, prevLimit)
            },
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
                return this.$t(templateStatusList[cellValue])
            },

            convertTime (row, column, cellValue, index) {
                return convertTime(cellValue)
            },
            toDetail () {
                this.$router.push({
                    name: 'details',
                    params: {
                        type: 'template',
                        code: this.$route.params.code
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .version-detail {
        padding: 20px;
    }
</style>

<template>
    <article>
        <section class="show-detail">
            <img v-if="detail.logoUrl" :src="detail.logoUrl" class="detail-img">
            <ul :class="[{ 'overflow': !hasShowAll }, 'detail-items']" ref="detail">
                <li class="detail-item">
                    <span class="item-name">{{ detail.imageName }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.标识') }}：</span>
                    <span>{{ detail.imageCode }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.范畴') }}：</span>
                    <span>{{ detail.categoryName }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.分类') }}：</span>
                    <span>{{ detail.classifyName }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.功能标签') }}：</span>
                    <label-list :label-list="detail.labelList.map(x => x.labelName)"></label-list>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.适用机器') }}：</span>
                    <label-list :label-list="detail.agentTypeScope" :formatter="agentFilter"></label-list>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.简介') }}：</span>
                    <span>{{ detail.summary || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.详细描述') }}：</span>
                    <mavon-editor
                        :editable="false"
                        default-open="preview"
                        :subfield="false"
                        :toolbars-flag="false"
                        :external-link="false"
                        :box-shadow="false"
                        preview-background="#fff"
                        v-model="detail.description"
                    />
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.镜像') }}：</span>
                    <span>{{(detail.imageRepoUrl ? detail.imageRepoUrl + '/' : '') + detail.imageRepoName + ':' + detail.imageTag}}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.镜像凭证') }}：</span>
                    <span>{{detail.ticketId}}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.发布者') }}：</span>
                    <span>{{detail.publisher}}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.发布类型') }}：</span>
                    <span>{{detail.releaseType|releaseFilter}}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.版本') }}：</span>
                    <span>{{detail.version}}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.发布描述') }}：</span>
                    <span>{{detail.versionContent}}</span>
                </li>
            </ul>
            <span class="summary-all" @click="hasShowAll = true" v-if="isOverDes && !hasShowAll"> {{ $t('展开全部') }} </span>
        </section>

        <section class="show-version">
            <span class="version-label">{{ $t('store.版本列表') }}</span>
            <bk-button theme="primary"
                class="version-button"
                :disabled="disableAddVersion"
                @click="$router.push({ name: 'editImage', params: { imageId: versionList[0].imageId } })"
            > {{ $t('store.新增版本') }} </bk-button>
            <bk-table :data="versionList"
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#fff' }"
                :pagination="pagination"
                @page-change="(page) => $emit('pageChanged', page)"
                @page-limit-change="(currentLimit, prevLimit) => $emit('pageLimitChanged', currentLimit, prevLimit)"
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
        </section>
    </article>
</template>

<script>
    import { imageStatusList } from '@/store/constants'
    import { convertTime } from '@/utils/index'
    import labelList from '../../../labelList'

    export default {
        filters: {
            releaseFilter (value) {
                const local = window.devops || {}
                let res = ''
                switch (value) {
                    case 'NEW':
                        res = local.$t('store.初始化')
                        break
                    case 'INCOMPATIBILITY_UPGRADE':
                        res = local.$t('store.非兼容升级')
                        break
                    case 'COMPATIBILITY_UPGRADE':
                        res = local.$t('store.兼容式功能更新')
                        break
                    case 'COMPATIBILITY_FIX':
                        res = local.$t('store.兼容式问题修正')
                        break
                }
                return res
            }
        },

        components: {
            labelList
        },

        props: {
            detail: Object,
            versionList: Array,
            pagination: Object
        },

        data () {
            return {
                progressStatus: ['COMMITTING', 'CHECKING', 'CHECK_FAIL', 'TESTING', 'AUDITING'],
                upgradeStatus: ['AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION'],
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
                }
            }
        },

        computed: {
            disableAddVersion () {
                const firstVersion = this.versionList[0] || {}
                return this.upgradeStatus.indexOf(firstVersion.atomStatus) === -1
            }
        },

        mounted () {
            this.$nextTick(() => {
                this.isOverDes = this.$refs.detail.scrollHeight > 290
            })
        },

        methods: {
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

            agentFilter (value) {
                const local = window.devops || {}
                let res = ''
                switch (value) {
                    case 'DOCKER':
                        res = local.$t('store.Devnet 物理机')
                        break
                    case 'IDC':
                        res = 'IDC CVM'
                        break
                    case 'PUBLIC_DEVCLOUD':
                        res = 'DevCloud'
                        break
                }
                return res
            },

            statusFormatter (row, column, cellValue, index) {
                return this.$t(imageStatusList[cellValue])
            },

            convertTime (row, column, cellValue, index) {
                return convertTime(cellValue)
            },

            routerProgress (id) {
                this.$router.push({
                    name: 'releaseProgress',
                    params: {
                        releaseType: 'upgrade',
                        atomId: id
                    }
                })
            },

            editAtom (routerName, id) {
                this.$router.push({
                    name: routerName,
                    params: {
                        atomId: id
                    }
                })
            }
        }
    }
</script>

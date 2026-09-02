<template>
    <section class="show-version g-scroll-pagination-table">
        <div class="version-button">
            <bk-button
                theme="primary"
                :disabled="!canAddMainVersion"
                @click="editAtom('upgradeAtom', versionList[0].atomId)"
            >
                {{ $t('store.新增版本') }}
            </bk-button>
            <!-- ALL-全部 FORMAL-正式版本 TEST-测试版本，默认正式版本 -->
            <ul class="version-tab">
                <li
                    v-for="(item, idx) in versionTab"
                    :key="idx"
                    :class="[versionType === item.type ? 'active' : '']"
                    @click="handleChangeVersionType(item.type)"
                >
                    <span>{{ $t(item.i18nKey) }}</span>
                </li>
            </ul>
        </div>
        <bk-table
            :data="versionList"
            :outer-border="false"
            :header-border="false"
            :header-cell-style="{ background: '#fff' }"
            v-bkloading="{ isLoading: loading }"
            :pagination="pagination"
            @page-change="(page) => $emit('pageChanged', page)"
            @page-limit-change="(currentLimit, prevLimit) => $emit('pageLimitChanged', currentLimit, prevLimit)"
        >
            <bk-table-column
                :label="$t('store.版本')"
                show-overflow-tooltip
            >
                <template slot-scope="props">
                    <span>{{ props.row.version || 'init' }}</span>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('store.状态')"
                prop="atomStatus"
                :formatter="statusFormatter"
                show-overflow-tooltip
            ></bk-table-column>
            <bk-table-column
                :label="$t('store.创建人')"
                prop="creator"
                show-overflow-tooltip
            ></bk-table-column>
            <bk-table-column
                :label="$t('store.创建时间')"
                prop="createTime"
                show-overflow-tooltip
            ></bk-table-column>
            <bk-table-column
                :label="$t('store.操作')"
                width="150"
                class-name="handler-btn"
            >
                <template slot-scope="props">
                    <section v-show="!index">
                        <span
                            class="update-btn"
                            @click="showDetail(props.row.atomId)"
                        >{{ $t('store.查看') }}</span>
                        <span
                            class="update-btn"
                            v-if="props.row.atomStatus === 'INIT'"
                            @click="editAtom('shelfAtom', props.row.atomId)"
                        > {{ $t('store.上架') }} </span>
                        <span
                            class="update-btn"
                            v-if="progressStatus.indexOf(props.row.atomStatus) > -1"
                            @click="routerProgress(props.row.atomId)"
                        > {{ $t('store.进度') }} </span>
                        <span
                            class="update-btn"
                            v-if="props.row.atomStatus === 'RELEASED'"
                            @click="offlineAtom(props.row)"
                        > {{ $t('store.下架') }} </span>
                    </section>
                </template>
            </bk-table-column>
        </bk-table>

        <bk-sideslider
            class="offline-atom-slider"
            :is-show.sync="offlineObj.show"
            :title="offlineObj.title"
            :quick-close="offlineObj.quickClose"
            :width="offlineObj.width"
        >
            <template slot="content">
                <bk-form
                    :label-width="100"
                    :model="offlineObj.form"
                    class="manage-version-offline"
                    ref="offlineForm"
                >
                    <bk-form-item :label="$t('store.名称')">
                        {{ offlineObj.form.name }}
                    </bk-form-item>
                    <bk-form-item :label="$t('store.标识')">
                        {{ offlineObj.form.atomCode }}
                    </bk-form-item>
                    <bk-form-item :label="$t('store.版本')">
                        {{ offlineObj.form.version }}
                    </bk-form-item>
                    <bk-form-item
                        :label="$t('store.下架原因')"
                        :required="true"
                        property="reason"
                        :rules="[requireRule($t('store.下架原因'))]"
                        error-display-type="normal"
                    >
                        <bk-input
                            type="textarea"
                            :rows="3"
                            :maxlength="255"
                            v-model="offlineObj.form.reason"
                        ></bk-input>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button
                            theme="primary"
                            @click="submitofflineAtom"
                            :loading="offlineObj.loading"
                        >
                            {{ $t('store.提交') }}
                        </bk-button>
                    </bk-form-item>
                </bk-form>
            </template>
        </bk-sideslider>

        <bk-sideslider
            quick-close
            class="offline-atom-slider"
            :is-show.sync="hasShowDetail"
            :title="$t('store.查看详情')"
            :width="800"
        >
            <template slot="content">
                <atom-detail
                    :detail="detail"
                    v-bkloading="{ isLoading: detailLoading }"
                    class="version-detail"
                >
                    <li class="detail-item">
                        <span class="detail-label">{{ $t('store.发布者：') }}</span>
                        <span>{{ detail.publisher || '--' }}</span>
                    </li>
                    <li class="detail-item">
                        <span class="detail-label">{{ $t('store.发布类型：') }}</span>
                        <span>{{ releaseMap[detail.releaseType] || '--' }}</span>
                    </li>
                    <li class="detail-item">
                        <span class="detail-label">{{ $t('store.版本：') }}</span>
                        <span>{{ detail.version || '--' }}</span>
                    </li>
                    <li class="detail-item">
                        <span class="detail-label">{{ $t('store.版本日志：') }}</span>
                        <mavon-editor
                            :editable="false"
                            default-open="preview"
                            :subfield="false"
                            :toolbars-flag="false"
                            :external-link="false"
                            :box-shadow="false"
                            preview-background="#fff"
                            :language="mavenLang"
                            v-model="detail.versionContent"
                        />
                    </li>
                </atom-detail>
            </template>
        </bk-sideslider>
    </section>
</template>

<script>
    import { atomStatusMap } from '@/store/constants'
    import atomDetail from '../../detail/atom-detail/show.vue'

    export default {
        components: {
            atomDetail
        },

        props: {
            versionList: Array,
            pagination: Object,
            versionType: {
                type: String,
                default: 'FORMAL'
            },
            loading: {
                type: Boolean,
                default: false
            },
            canAddMainVersion: {
                type: Boolean,
                default: true
            }
        },

        data () {
            return {
                progressStatus: ['COMMITTING', 'BUILDING', 'BUILD_FAIL', 'TESTING', 'AUDITING'],
                versionTab: [
                    { i18nKey: '全部', type: 'ALL' },
                    { i18nKey: '正式版本', type: 'FORMAL' },
                    { i18nKey: '测试版本', type: 'TEST' }
                ],
                offlineObj: {
                    show: false,
                    title: this.$t('store.下架插件版本'),
                    quickClose: true,
                    width: 565,
                    loading: false,
                    form: {
                        name: '',
                        atomCode: '',
                        reason: '',
                        version: ''
                    }
                },
                hasShowDetail: false,
                detailLoading: false,
                detail: {},
                releaseMap: {
                    NEW: this.$t('store.新上架'),
                    INCOMPATIBILITY_UPGRADE: this.$t('store.非兼容式升级'),
                    COMPATIBILITY_UPGRADE: this.$t('store.兼容式功能更新'),
                    COMPATIBILITY_FIX: this.$t('store.兼容式问题修正')
                }
            }
        },

        computed: {
            atomStatusList () {
                return atomStatusMap
            },

            mavenLang () {
                return this.$i18n.locale === 'en-US' ? 'en' : this.$i18n.locale
            }
        },

        methods: {
            handleChangeVersionType (type) {
                if (this.versionType === type) return
                this.$emit('versionTypeChanged', type)
            },

            showDetail (atomId) {
                this.hasShowDetail = true
                this.detailLoading = true
                this.$store.dispatch('store/requestAtomDetail', { atomId }).then((res) => {
                    this.detail = res || {}
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.detailLoading = false
                })
            },

            statusFormatter (row, column, cellValue, index) {
                return this.$t(this.atomStatusList[cellValue])
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
            },

            offlineAtom (row) {
                this.offlineObj.show = true
                this.offlineObj.form.name = row.name
                this.offlineObj.form.atomCode = row.atomCode
                this.offlineObj.form.reason = ''
                this.offlineObj.form.version = row.version
            },

            submitofflineAtom () {
                this.$refs.offlineForm.validate().then(() => {
                    this.offlineObj.loading = true
                    this.$store.dispatch('store/offlineAtom', {
                        atomCode: this.offlineObj.form.atomCode,
                        params: this.offlineObj.form
                    }).then(() => {
                        this.$emit('pageChanged')
                        this.$bkMessage({ message: this.$t('store.提交成功'), theme: 'success' })
                    }).catch((err) => {
                        this.$bkMessage({ message: err.message || err, theme: 'error' })
                    }).finally(() => {
                        this.offlineObj.loading = false
                        this.offlineObj.show = false
                    })
                }, (validator) => {
                    this.$bkMessage({ message: validator.content || validator, theme: 'error' })
                })
            },

            requireRule (name) {
                return {
                    required: true,
                    message: this.$t('store.validateMessage', [name, this.$t('store.必填项')]),
                    trigger: 'blur'
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .manage-version-offline, .version-detail {
        padding: 20px;
    }
    .version-button {
        display: flex;
        align-items: center;

        .version-tab {
            display: flex;
            align-items: center;
            padding: 4px;
            margin-left: 8px;
            background: #EAEBF0;
            height: 32px;
            border-radius: 2px;

            li {
                display: flex;
                align-items: center;
                height: 24px;
                font-size: 12px;
                color: #4D4F56;
                text-align: center;
                border-radius: 2px;
                cursor: pointer;

                span {
                    display: flex;
                    align-items: center;
                    padding: 0 12px;
                    height: 14px;
                }

                &:not(:last-child) span {
                    border-right: 1px solid #e0e2e8;
                }
            }

            .active {
                background-color: #fff;
                color: #3a84ff;
                border-radius: 2px;

                &:not(:last-child) span {
                    border-right: none;
                }
            }
        }
    }
    ::v-deep .bk-sideslider-content {
        max-height: calc(100% - 60px) !important;
    }
</style>

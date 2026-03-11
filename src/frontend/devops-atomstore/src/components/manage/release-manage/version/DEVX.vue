<template>
    <section class="show-version g-scroll-table">
        <bk-button
            theme="primary"
            class="version-button"
            :disabled="disableAddVersion"
            @click="addVersion"
        >
            {{ $t('store.新增版本') }}
        </bk-button>
        <bk-table
            :data="versionList"
            :outer-border="false"
            :header-border="false"
            :header-cell-style="{ background: '#fff' }"
            :pagination="pagination"
            @page-change="(page) => $emit('pageChanged', page)"
            @page-limit-change="
                (currentLimit, prevLimit) => $emit('pageLimitChanged', currentLimit, prevLimit)
            "
        >
            <bk-table-column
                :label="$t('store.版本')"
                prop="version"
            ></bk-table-column>
            <bk-table-column
                :label="$t('store.状态')"
                prop="status"
                :formatter="statusFormatter"
            ></bk-table-column>
            <bk-table-column
                :label="$t('store.创建人')"
                prop="creator"
            ></bk-table-column>
            <bk-table-column
                :label="$t('store.创建时间')"
                prop="createTime"
                :formatter="convertTime"
            ></bk-table-column>
            <bk-table-column
                :label="$t('store.操作')"
                width="150"
                class-name="handler-btn"
            >
                <template slot-scope="props">
                    <span
                        class="update-btn"
                        @click="showDetail(props.row)"
                    >{{ $t('store.查看') }}</span>
                    <span
                        class="update-btn"
                        v-if="PROGRESS_STATUS.includes(props.row.status)"
                        @click="goProgressDetail(props.row)"
                    >{{ $t('store.进度') }}</span>
                    <span
                        class="update-btn"
                        v-if="props.row.status === 'RELEASED'"
                        @click="showTakedown(props.row)"
                    >{{ $t('store.下架') }}</span>
                </template>
            </bk-table-column>
        </bk-table>

        <!-- 查看详情侧边栏 -->
        <bk-sideslider
            quick-close
            class="offline-atom-slider"
            :is-show.sync="hasShowDetail"
            :title="$t('store.查看详情')"
            :width="800"
        >
            <template slot="content">
                <div
                    v-bkloading="{ isLoading: detailLoading }"
                    class="version-detail"
                >
                    <div
                        class="detail-container"
                        v-if="detail.storeId"
                    >
                        <img
                            :src="detail.logoUrl"
                            class="detail-logo"
                        />
                        <div class="detail-content">
                            <h2 class="detail-title">{{ detail.name }}</h2>
                            <ul class="detail-list">
                                <li class="detail-item">
                                    <span class="detail-label">{{ $t('store.标识') }}：</span>
                                    <span>{{ detail.storeCode || '--' }}</span>
                                </li>
                                <li class="detail-item">
                                    <span class="detail-label">{{ $t('store.发布包') }}：</span>
                                    <span>{{ detail.packageName || '--' }}</span>
                                </li>
                                <li class="detail-item">
                                    <span class="detail-label">{{ $t('store.简介') }}：</span>
                                    <span>{{ detail.summary || '--' }}</span>
                                </li>
                                <li class="detail-item">
                                    <span class="detail-label">{{ $t('store.上行带宽峰值') }}：</span>
                                    <span>{{ detail.extData?.netPolicyInfo?.maxPeakBandwidth || 0 }}</span>
                                </li>
                                <li class="detail-item">
                                    <span class="detail-label">{{ $t('store.下行带宽峰值') }}：</span>
                                    <span>{{ detail.extData?.netPolicyInfo?.minPeakBandwidth || 0 }}</span>
                                </li>
                                <li
                                    class="detail-item"
                                    v-if="
                    detail.extData?.netPolicyInfo?.needVisitedSiteInfos &&
                    detail.extData.netPolicyInfo.needVisitedSiteInfos.length
                                    "
                                >
                                    <span class="detail-label">{{ $t('store.需要访问的站点') }}：</span>
                                    <site-info-table
                                        :site-infos="detail.extData.netPolicyInfo.needVisitedSiteInfos"
                                        :editable="false"
                                    />
                                </li>
                                <li class="detail-item">
                                    <span class="detail-label">Scheme：</span>
                                    <span>{{ detail.extData?.urlScheme || '--' }}</span>
                                </li>
                                <li class="detail-item">
                                    <span class="detail-label">{{ $t('store.发布者') }}：</span>
                                    <span>{{ detail.versionInfo?.publisher || '--' }}</span>
                                </li>
                                <li class="detail-item">
                                    <span class="detail-label">{{ $t('store.发布类型') }}：</span>
                                    <span>{{ $t(detail.versionInfo?.releaseType) }}</span>
                                </li>
                                <li class="detail-item">
                                    <span class="detail-label">{{ $t('store.版本') }}：</span>
                                    <span>{{ detail.versionInfo?.version || '--' }}</span>
                                </li>
                                <li class="detail-item">
                                    <span class="detail-label">{{ $t('store.详细描述') }}：</span>
                                    <mavon-editor
                                        v-if="detail.description"
                                        :editable="false"
                                        default-open="preview"
                                        :subfield="false"
                                        :toolbars-flag="false"
                                        :external-link="false"
                                        :box-shadow="false"
                                        preview-background="#fff"
                                        v-model="detail.description"
                                    />
                                    <span v-else>--</span>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
            </template>
        </bk-sideslider>

        <!-- 下架侧边栏 -->
        <bk-sideslider
            :is-show.sync="hasShowTakedown"
            :title="$t('store.下架应用')"
            :width="666"
            quick-close
        >
            <template slot="content">
                <div class="takedown-content">
                    <ul class="info-list">
                        <li class="info-item">
                            <label class="info-label">{{ $t('store.名称') }}：</label>
                            <span>{{ takedownRow.name || '--' }}</span>
                        </li>
                        <li class="info-item">
                            <label class="info-label">{{ $t('store.标识') }}：</label>
                            <span>{{ takedownRow.storeCode || '--' }}</span>
                        </li>
                        <li class="info-item">
                            <label class="info-label">{{ $t('store.版本') }}：</label>
                            <span>{{ takedownRow.version || '--' }}</span>
                        </li>
                    </ul>
                    <bk-form
                        :label-width="100"
                        :model="takedownForm"
                    >
                        <bk-form-item
                            :label="$t('store.下架原因')"
                            required
                            property="reason"
                        >
                            <bk-input
                                type="textarea"
                                v-model="takedownForm.reason"
                                :placeholder="$t('store.请输入下架原因')"
                                :rows="5"
                            ></bk-input>
                        </bk-form-item>
                    </bk-form>
                    <bk-button
                        theme="primary"
                        :loading="isSubmiting"
                        :disabled="isSubmiting"
                        :title="$t('store.请输入下架原因')"
                        @click="confirmTakedown"
                        style="margin-left: 100px; margin-top: 24px;"
                    >
                        {{ $t('store.下架') }}
                    </bk-button>
                </div>
            </template>
        </bk-sideslider>
    </section>
</template>

<script>
    import { convertTime } from '@/utils/index'
    import { STORE_TYPE, PROGRESS_STATUS, UPGRADE_STATUS } from '@/utils/constants'
    import SiteInfoTable from '@/components/siteInfoTable.vue'

    export default {
        components: {
            SiteInfoTable,
        },

        props: {
            versionList: Array,
            pagination: Object,
        },

        data () {
            return {
                hasShowDetail: false,
                detailLoading: false,
                detail: {},
                hasShowTakedown: false,
                isSubmiting: false,
                takedownRow: {},
                takedownForm: {
                    storeCode: '',
                    storeType: STORE_TYPE,
                    version: '',
                    reason: '',
                },
            }
        },

        computed: {
            disableAddVersion () {
                const firstVersion = this.versionList[0] || {}
                return !this.UPGRADE_STATUS.includes(firstVersion.status)
            },
        },

        created () {
            this.UPGRADE_STATUS = UPGRADE_STATUS
            this.PROGRESS_STATUS = PROGRESS_STATUS
        },

        methods: {
            addVersion () {
                const firstVersion = this.versionList[0]
                this.$router.push({
                    name: 'addReleaseVersion',
                    params: {
                        storeCode: this.$route.params.code,
                        storeId: firstVersion?.storeId,
                    },
                })
            },

            showDetail (row) {
                this.hasShowDetail = true
                this.detailLoading = true
                this.$store
                    .dispatch('store/getComponentDetailByVersion', {
                        storeId: row.storeId,
                    })
                    .then((res) => {
                        this.detail = res || {}
                    })
                    .catch((err) => {
                        this.$bkMessage({ message: err.message || err, theme: 'error' })
                    })
                    .finally(() => {
                        this.detailLoading = false
                    })
            },

            goProgressDetail (row) {
                this.$router.push({
                    name: 'progressDetail',
                    params: {
                        storeCode: row.storeCode,
                        storeId: row.storeId,
                    },
                })
            },

            showTakedown (row) {
                this.takedownRow = row
                this.takedownForm = {
                    storeCode: row.storeCode,
                    storeType: STORE_TYPE,
                    version: row.version,
                    reason: '',
                }
                this.hasShowTakedown = true
            },

            async confirmTakedown () {
                if (!this.takedownForm.reason) {
                    this.$bkMessage({
                        message: this.$t('store.请输入下架原因'),
                        theme: 'warning',
                    })
                    return
                }

                try {
                    this.isSubmiting = true
                    await this.$store.dispatch('store/takeDownVersion', this.takedownForm)
                    this.$bkMessage({
                        message: this.$t('store.下架成功'),
                        theme: 'success',
                    })
                    this.hasShowTakedown = false
                    this.takedownForm.reason = ''
                    // 刷新列表
                    this.$emit('pageChanged', this.pagination.current)
                } catch (err) {
                    this.$bkMessage({
                        message: err.message || err,
                        theme: 'error',
                    })
                } finally {
                    this.isSubmiting = false
                }
            },

            statusFormatter (row, column, cellValue, index) {
                return this.$t(cellValue) || cellValue
            },

            convertTime (row, column, cellValue, index) {
                return convertTime(cellValue)
            },
        },
    }
</script>

<style lang="scss" scoped>
.version-detail {
  padding: 20px;
  display: flex;
  align-items: flex-start;
  min-height: calc(100vh - 100px);
  overflow-y: auto;
}

.detail-container {
  display: flex;
  align-items: flex-start;
  width: 100%;
}

.detail-logo {
  width: 100px;
  height: 100px;
  margin-right: 20px;
  flex-shrink: 0;
  object-fit: contain;
}

.detail-content {
  flex: 1;
  min-width: 0;
}

.detail-title {
  font-size: 20px;
  color: #222;
  margin: 0 0 18px 0;
  font-weight: 600;
}

.detail-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.detail-item {
  font-size: 14px;
  margin-top: 18px;
  display: flex;
  align-items: flex-start;

  &:first-child {
    margin-top: 0;
  }

  .detail-label {
    min-width: 150px;
    color: #999;
    margin-right: 10px;
    flex-shrink: 0;
  }

  .visited-site-table {
    flex: 1;
    margin-top: 10px;
  }
}

.offline-atom-slider {
  ::v-deep .bk-sideslider-wrapper {
    overflow: hidden;
  }
  ::v-deep .bk-sideslider-content {
    height: calc(100% - 50px);
  }
}

.takedown-content {
  padding: 24px;

  .info-list {
    list-style: none;
    padding: 0;
    margin: 0 0 24px 0;

    .info-item {
      display: flex;
      align-items: center;
      margin-bottom: 20px;

      .info-label {
        flex-shrink: 0;
        width: 100px;
        padding-right: 30px;
        text-align: right;
        font-weight: 600;
        color: #63656e;
      }
    }
  }
}
::v-deep .v-note-wrapper {
  max-height: 500px;
  width: 100%;
  border: 1px solid #c4c6cc;

  .v-note-panel {
    box-shadow: none;
  }
}
</style>

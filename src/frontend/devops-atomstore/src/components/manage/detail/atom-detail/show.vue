<template>
    <article>
        <section class="show-detail">
            <img v-if="detail.logoUrl" :src="detail.logoUrl" class="detail-img">
            <ul :class="[{ 'overflow': !hasShowAll }, 'detail-items']" ref="detail">
                <li class="detail-item">
                    <span class="item-name">{{ detail.name }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.标识') }}：</span>
                    <span>{{ detail.atomCode }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.范畴') }}：</span>
                    <span>{{ categoryMap[detail.category] }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.分类') }}：</span>
                    <span>{{ detail.classifyName }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.适用机器类型') }}：</span>
                    <div v-if="detail.os">{{ jobTypeMap[detail.jobType] }}
                        <span v-if="detail.jobType === 'AGENT'">（
                            <i class="devops-icon icon-linux-view" v-if="detail.os.indexOf('LINUX') !== -1"></i>
                            <i class="devops-icon icon-windows" v-if="detail.os.indexOf('WINDOWS') !== -1"></i>
                            <i class="devops-icon icon-macos" v-if="detail.os.indexOf('MACOS') !== -1"></i>）
                        </span>
                    </div>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.发布包') }}：</span>
                    <span>{{ detail.pkgName || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.功能标签') }}：</span>
                    <label-list :label-list="detail.labelList.map(x => x.labelName)"></label-list>
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
            </ul>
            <span class="summary-all" @click="hasShowAll = true" v-if="isOverDes && !hasShowAll"> {{ $t('展开全部') }} </span>
        </section>

        <section class="show-version">
            <span class="version-label">{{ $t('store.版本列表') }}</span>
            <bk-button theme="primary"
                class="version-button"
                :disabled="disableAddVersion"
                @click="editAtom('upgradeAtom', versionList[0].atomId)"
            > {{ $t('store.新增版本') }} </bk-button>
            <bk-table :data="versionList" :outer-border="false" :header-border="false" :header-cell-style="{ background: '#fff' }">
                <bk-table-column :label="$t('store.版本')" prop="version"></bk-table-column>
                <bk-table-column :label="$t('store.状态')" prop="atomStatus" :formatter="statusFormatter"></bk-table-column>
                <bk-table-column :label="$t('store.创建人')" prop="creator"></bk-table-column>
                <bk-table-column :label="$t('store.创建时间')" prop="createTime"></bk-table-column>
                <bk-table-column :label="$t('store.操作')" width="120" class-name="handler-btn">
                    <template slot-scope="props">
                        <section v-show="!index">
                            <span class="update-btn" v-if="props.row.atomStatus === 'INIT'" @click="editAtom('shelfAtom', props.row.atomId)"> {{ $t('store.上架') }} </span>
                            <span class="update-btn"
                                v-if="progressStatus.indexOf(props.row.atomStatus) > -1" @click="routerProgress(props.row.atomId)"> {{ $t('store.进度') }} </span>
                        </section>
                    </template>
                </bk-table-column>
            </bk-table>
        </section>
    </article>
</template>

<script>
    import { atomStatusMap } from '@/store/constants'
    import labelList from '../../../labelList'

    export default {
        components: {
            labelList
        },

        props: {
            detail: Object,
            versionList: Array
        },

        data () {
            return {
                categoryMap: {
                    'TASK': this.$t('store.流水线插件'),
                    'TRIGGER': this.$t('store.流水线触发器')
                },
                jobTypeMap: {
                    'AGENT': this.$t('store.编译环境'),
                    'AGENT_LESS': this.$t('store.无编译环境')
                },
                progressStatus: ['COMMITTING', 'BUILDING', 'BUILD_FAIL', 'TESTING', 'AUDITING'],
                upgradeStatus: ['AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION'],
                isOverDes: false,
                hasShowAll: false
            }
        },

        computed: {
            atomStatusList () {
                return atomStatusMap
            },

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
            }
        }
    }
</script>

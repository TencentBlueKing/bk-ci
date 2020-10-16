<template>
    <section class="show-version g-scroll-table">
        <bk-button theme="primary"
            class="version-button"
            :disabled="disableAddVersion"
            @click="editAtom('upgradeAtom', versionList[0].atomId)"
        > {{ $t('store.新增版本') }} </bk-button>
        <bk-table :data="versionList" :outer-border="false" :header-border="false" :header-cell-style="{ background: '#fff' }">
            <bk-table-column :label="$t('store.版本')">
                <template slot-scope="props">
                    <span>{{ props.row.version || 'init' }}</span>
                </template>
            </bk-table-column>
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
</template>

<script>
    import { atomStatusMap } from '@/store/constants'

    export default {
        props: {
            versionList: Array
        },

        data () {
            return {
                progressStatus: ['COMMITTING', 'BUILDING', 'BUILD_FAIL', 'TESTING', 'AUDITING'],
                upgradeStatus: ['UNDERCARRIAGED', 'AUDIT_REJECT', 'RELEASED', 'GROUNDING_SUSPENSION']
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

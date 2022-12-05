<template>
    <article class="service-manage-home" v-bkloading="{ isLoading }">
        <nav class="service-nav">
            <span>{{ $t('store.微扩展管理') }}</span>
            <bk-button @click="goToStore" theme="primary">{{ $t('store.添加微扩展服务') }}</bk-button>
        </nav>
        <header class="service-filter">
            <bk-button ext-cls="filter-button"
                :disabled="filterList.length <= 0"
                @click="filterList = []"
            >{{ $t('store.全部微扩展') }}（{{installList.length}}）
            </bk-button>
            <bk-checkbox-group v-model="filterList">
                <ul class="fliter-list">
                    <li v-for="(group, index) in serviceGroup" :key="index" class="filter-item">
                        <template v-if="group.childItem.length">
                            <logo
                                class="service-logo"
                                :name="getIcon(group.extServiceItem.id)"
                                size="18"
                            />
                            <section class="filter-select">
                                <bk-checkbox :value="service.id" v-for="service in group.childItem" :key="service.id">{{service.name}}</bk-checkbox>
                            </section>
                        </template>
                    </li>
                </ul>
            </bk-checkbox-group>
        </header>
        <main class="service-table">
            <bk-table :empty-text="$t('store.暂时没有微扩展')"
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#fff' }"
                :data="filterInstallList"
                :show-overflow-tooltip="true"
            >
                <bk-table-column :label="$t('store.微扩展名称')" prop="serviceName"></bk-table-column>
                <bk-table-column :label="$t('store.发布者')" prop="publisher"></bk-table-column>
                <bk-table-column :label="$t('store.版本')" prop="version"></bk-table-column>
                <bk-table-column :label="$t('store.状态')" prop="serviceStatus" :formatter="statusFormatter"></bk-table-column>
                <bk-table-column :label="$t('store.操作人')" prop="creator"></bk-table-column>
                <bk-table-column :label="$t('store.操作时间')" prop="publishTime" width="180" :formatter="timeFormatter"></bk-table-column>
                <bk-table-column :label="$t('store.操作')" width="150">
                    <template slot-scope="props">
                        <span v-bk-tooltips="{ content: $t('store.微扩展初始化项目，不能卸载'), disabled: props.row.isUninstall }">
                            <bk-button text @click="uninstall(props.row)" :disabled="!props.row.isUninstall">
                                {{$t('store.卸载')}}
                            </bk-button>
                        </span>
                    </template>
                </bk-table-column>
            </bk-table>
        </main>
    </article>
</template>

<script>
    import { serviceStatusMap } from '../store/constants'

    export default {
        data () {
            return {
                isLoading: true,
                filterList: [],
                serviceGroup: [],
                installList: []
            }
        },

        computed: {
            filterInstallList () {
                return this.installList.filter((install) => {
                    return this.filterList.length <= 0 || this.filterList.some(id => install.itemIds.includes(id))
                })
            }
        },

        created () {
            this.initData()
        },

        methods: {
            initData () {
                const projectCode = this.$route.params.projectCode
                Promise.all([
                    this.$store.dispatch('store/requesInstalledServiceList', projectCode),
                    this.$store.dispatch('store/requestServiceItemList')
                ]).then(([installList, serviecList]) => {
                    const itemIds = []
                    installList.forEach(x => itemIds.push(...x.itemIds))
                    this.serviceGroup = (serviecList || []).map((service) => {
                        service.childItem = service.childItem.filter((item) => {
                            return itemIds.includes(item.id)
                        })
                        return service
                    })
                    this.installList = installList || []
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isLoading = false))
            },

            uninstall (row) {
                const projectCode = this.$route.params.projectCode
                const postData = {
                    projectCode,
                    serviceCode: row.serviceCode
                }
                const confirmFn = () => {
                    this.isLoading = true
                    this.$store.dispatch('store/uninstallService', postData).then(() => {
                        this.initData()
                    }).catch((err) => {
                        this.$bkMessage({ message: err.message || err, theme: 'error' })
                    }).finally(() => (this.isLoading = false))
                }

                this.$bkInfo({
                    type: 'warning',
                    title: this.$t('store.确定卸载', [row.serviceName]),
                    confirmFn
                })
            },

            getIcon (id) {
                const serviceObject = window.serviceObject || {}
                const serviceMap = serviceObject.serviceMap || {}
                const keys = Object.keys(serviceMap)
                let link = ''
                keys.forEach((key) => {
                    const cur = serviceMap[key]
                    if (+cur.id === +id) link = cur.link_new
                })
                return link.replace(/\/?(devops\/)?(\w+)\S*$/, '$2')
            },

            timeFormatter (row, column, cellValue, index) {
                const date = new Date(+cellValue)
                const year = date.toISOString().slice(0, 10)
                const time = date.toTimeString().split(' ')[0]
                return `${year} ${time}`
            },

            statusFormatter (row, column, cellValue, index) {
                return this.$t(serviceStatusMap[cellValue])
            },

            goToStore () {
                this.$router.push({
                    name: 'atomHome',
                    query: {
                        pipeType: 'service'
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .service-manage-home {
        background: #ebedf0;
        display: flex;
        flex-direction: column;
        align-items: center;
        flex: 1;
        .service-nav {
            width: 100%;
            min-height: 56px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0 27px;
            background: #ffffff;
            box-shadow: 0pt 1pt 0pt 0pt #ebedf0;
            font-size: 16px;
            color: #222222;
            line-height: 16px;
        }
        .service-filter {
            width: 94%;
            max-width: 1400px;
            margin: 32px auto 16px;
            background: #ffffff;
            box-shadow: 1px 2px 3px 0px rgba(0,0,0,0.05);
            .filter-button {
                margin: 32px;
            }
            .fliter-list {
                margin: 0 32px;
                .filter-item {
                    display: flex;
                    align-items: top;
                    .item-name {
                        display: inline-block;
                        width: 100px;
                        line-height: 18px;
                        font-size: 14px;
                    }
                    .filter-select {
                        margin-left: 20px;
                        ::v-deep  .bk-form-checkbox {
                            margin: 0 18px 20px 0;
                            .bk-checkbox-text {
                                width: 80px;
                            }
                        }
                    }
                }
            }
        }
        .service-table {
            flex: 1;
            width: 94%;
            max-width: 1400px;
            padding: 15px 32px;
            margin-bottom: 32px;
            background: #ffffff;
            box-shadow: 1px 2px 3px 0px rgba(0,0,0,0.05);
        }
    }
</style>

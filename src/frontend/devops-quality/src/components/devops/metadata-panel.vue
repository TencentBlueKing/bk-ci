<template>
    <div class="metadata-select-panel" v-bkloading="{ isLoading: loading }">
        <div class="metadata-panel-container">
            <div :class="{
                'metadata-main-tab': true,
                'indicator-list-tab': isIndexList
            }" v-if="!isCtrPointPanel">
                <bk-tab :active="currentTab" type="unborder-card" @tab-change="changeTab">
                    <bk-tab-panel
                        v-for="(panel, index) in panels"
                        v-bind="panel"
                        :key="index"
                    ></bk-tab-panel>
                </bk-tab>
            </div>
            <section class="metadata-main-content">
                <!-- 控制点选择 -->
                <template v-if="isCtrPointPanel">
                    <div class="property-item-wrapper"
                        v-for="(classify, index) in metaTree" :key="index">
                        <div class="classify-header"><span class="title">{{classify.stage}}</span></div>
                        <div class="control-point-wrapper" v-for="(atom, childIndex) in classify.controlPoints" :key="childIndex">
                            <div :class="{
                                     'proprety-item-contnet': true,
                                     'control-point-content': true,
                                     'optional-item': !atom.isSelected
                                 }"
                                v-if="atom.isDisplay">
                                <div class="info-title">
                                    <icon :name="getAtomIcon(atom.type)" size="24" style="fill:#C3CDD7" /><span class="atom-name">{{atom.name}}</span>
                                </div>
                                <div class="handle-btn selected-btn" v-if="atom.isSelected">{{$t('quality.已选择')}}</div>
                                <div class="handle-btn select-btn" v-if="!atom.isSelected"
                                    @click="selectNode(atom, 'controlPoint')">{{$t('quality.选择')}}
                                </div>
                            </div>
                        </div>
                    </div>
                </template>

                <!-- 指标集选择 -->
                <template v-if="!isCtrPointPanel && isIndexList">
                    <div class="task-item-wrapper"
                        v-for="(task, index) in metaTree" :key="index">
                        <div :class="{
                                 'proprety-item-contnet': true,
                                 'task-item-content': true,
                                 'optional-item': isIndexList && !task.isSelected,
                                 'hover': task.isDropdownShow
                             }"
                            v-if="task.isDisplay"
                            @click="toggleDropdown(task.hashId)">
                            <div class="task-name" :title="task.name">{{task.name}}</div>
                            <div class="task-desc" :title="task.desc">{{task.desc}}</div>
                            <div class="dropdown-icon" :class="{ 'hide': !task.indicators.length }">
                                <i :class="{
                                    'devops-icon': true,
                                    'icon-angle-double-right': true,
                                    'icon-flip': task.isDropdownShow
                                }"></i>
                            </div>
                            <div class="handle-btn select-btn" v-if="isIndexList && !task.isSelected"
                                @click.stop="selectNode(task, 'indexList')">{{$t('quality.添加')}}</div>
                        </div>
                        <template v-if="task.isDropdownShow">
                            <div class="metadata-item-wrapper"
                                v-for="(metadata, metaIndex) in task.indicators" :key="metaIndex">
                                <div class="proprety-item-contnet metadata-item-content"
                                    v-if="task.indicators.length && metadata.isDisplay">
                                    <div class="meta-name" :title="metadata.cnName">{{metadata.cnName}}</div>
                                    <!-- <div class="meta-desc" :title="getIndicatorDesc(metadata.metadataList)">{{ getIndicatorDesc(metadata.metadataList) }}</div> -->
                                    <div class="meta-desc" :title="metadata.desc">{{metadata.desc}}</div>
                                    <div class="handle-btn selected-btn" v-if="metadata.isSelected">{{$t('quality.已选择')}}</div>
                                </div>
                            </div>
                        </template>
                    </div>
                </template>

                <!-- 单个指标选择 -->
                <template v-if="!isCtrPointPanel && !isIndexList">
                    <div class="property-item-wrapper"
                        v-for="(classify, index) in metaTree" :key="index">
                        <div class="classify-header"><span class="title">{{classify.stage}}</span></div>
                        <div class="control-point-wrapper" v-for="(atom, atomIndex) in classify.controlPoints" :key="atomIndex">
                            <div class="proprety-item-contnet control-point-content"
                                v-if="atom.isDisplay">
                                <div class="info-title">
                                    <icon :name="getAtomIcon(atom.controlPoint)" size="24" style="fill:#C3CDD7" /><span class="atom-name">{{atom.controlPointName}}</span>
                                </div>
                            </div>
                            <div class="task-item-wrapper" style="padding-left:10px;"
                                v-for="(task, taskIndex) in atom.details" :key="taskIndex">
                                <div :class="{
                                         'proprety-item-contnet': true,
                                         'task-item-content': true,
                                         'hover': task.isDropdownShow
                                     }"
                                    v-if="task.isDisplay || atom.isMatch"
                                    @click="toggleDropdown(task.hashId)">
                                    <div class="task-name" :title="task.detail">{{task.detail}}</div>
                                    <div class="task-desc" :title="task.desc">{{task.desc}}</div>
                                    <div class="dropdown-icon" :class="{ 'hide': !task.items.length }">
                                        <i :class="{
                                            'devops-icon': true,
                                            'icon-angle-double-right': true,
                                            'icon-flip': task.isDropdownShow
                                        }"></i>
                                    </div>
                                </div>
                                <template v-if="task.isDropdownShow">
                                    <div class="metadata-item-wrapper"
                                        v-for="(metadata, metaIndex) in task.items" :key="metaIndex">
                                        <div :class="{
                                                 'proprety-item-contnet': true,
                                                 'metadata-item-content': true,
                                                 'optional-item': !metadata.isSelected && !isIndexList
                                             }"
                                            v-if="task.items.length && metadata.isDisplay">
                                            <div class="meta-name" :title="metadata.cnName">{{metadata.cnName}}</div>
                                            <!-- <div class="meta-desc" :title="getIndicatorDesc(metadata.metadataList)">{{ getIndicatorDesc(metadata.metadataList) }}</div> -->
                                            <div class="meta-desc" :title="metadata.desc">{{metadata.desc}}</div>
                                            <div class="handle-btn selected-btn" v-if="metadata.isSelected">{{$t('quality.已选择')}}</div>
                                            <div class="handle-btn select-btn" v-if="!metadata.isSelected && !isIndexList"
                                                @click="selectNode(metadata, 'singleIndex')">{{$t('quality.添加')}}
                                            </div>
                                        </div>
                                    </div>
                                </template>
                            </div>
                        </div>
                    </div>
                </template>

            </section>
        </div>
    </div>
</template>

<script>
    export default {
        props: {
            isPanelShow: {
                type: Boolean,
                default: false
            },
            selectedAtom: {
                type: String,
                default: ''
            },
            searchKey: {
                type: String,
                default: ''
            },
            panelType: {
                type: String,
                default: 'index'
            },
            selectedMeta: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                isInit: false,
                loading: false,
                currentTab: 'indexList',
                metaTree: [],
                indicatorSetList: [],
                indicatorList: [],
                controlPointList: [],
                panels: [
                    { name: 'indexList', label: this.$t('quality.指标集') },
                    { name: 'singleIndex', label: this.$t('quality.单个指标') }
                ]
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            isCtrPointPanel () {
                return this.panelType === 'controlPoint'
            },
            isIndexList () {
                return this.currentTab === 'indexList'
            }
        },
        watch: {
            isPanelShow (newVal) {
                if (newVal && this.isCtrPointPanel) {
                    if (!this.isInit) {
                        this.requestControlPoint()
                    }
                    this.getMetaList(this.controlPointList, this.searchKey)
                } else if (newVal && !this.isCtrPointPanel && this.isIndexList) {
                    if (!this.isInit) {
                        this.requestIndicatorSet()
                    }
                    this.requestIndicatorSet()
                } else if (newVal && !this.isInit && !this.isCtrPointPanel && !this.isIndexList) {
                    this.requestIndicators()
                } else {
                    this.searchKey = ''
                    this.isInit = false
                }
            }
        },
        created () {
            this.isInit = true

            // 组件升级之前保留 升级后需注释
            // this.initData()

            // 兼容sideslider组件升级问题（关闭侧栏时组件被销毁）
            if (this.isCtrPointPanel) {
                this.requestControlPoint()
            } else if (!this.isCtrPointPanel && this.isIndexList) {
                this.requestIndicatorSet()
            } else if (!this.isInit && !this.isCtrPointPanel && !this.isIndexList) {
                this.requestIndicators()
            }
        },
        methods: {
            initData () {
                this.requestIndicatorSet()
                this.requestIndicators()
                this.requestControlPoint()
            },
            async requestIndicatorSet () {
                this.loading = true
                try {
                    const res = await this.$store.dispatch('quality/requestIndicatorSet')

                    this.indicatorSetList.splice(0, this.indicatorSetList.length)
                    if (res) {
                        res.forEach(item => {
                            this.indicatorSetList.push(item)
                        })
                    }
                    this.getIndicatortList(this.indicatorSetList, this.searchKey)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.loading = false
                }
            },
            async requestIndicators () {
                this.loading = true
                try {
                    const res = await this.$store.dispatch('quality/requestIndicators', {
                        projectId: this.projectId
                    })

                    this.indicatorList.splice(0, this.indicatorList.length)
                    if (res) {
                        res.forEach(item => {
                            this.indicatorList.push(item)
                        })
                    }
                    this.getMetaList(this.indicatorList, this.searchKey)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.loading = false
                }
            },
            async requestControlPoint () {
                this.loading = true
                try {
                    const res = await this.$store.dispatch('quality/requestControlPoint', { projectId: this.projectId })

                    this.controlPointList.splice(0, this.controlPointList.length)
                    if (res) {
                        res.forEach(item => {
                            this.controlPointList.push(item)
                        })
                    }
                    this.getMetaList(this.controlPointList, this.searchKey)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.loading = false
                }
            },
            getMetaList (container, searchKey) {
                container.forEach(item => {
                    item.controlPoints.forEach(atom => {
                        atom.isSelected = atom.type === this.selectedAtom // 控制点是否已选
                        atom.isMatch = false
                        if (searchKey) {
                            const atomName = this.isCtrPointPanel ? atom.name : atom.controlPointName
                            atom.isDisplay = this.matchStr(atomName, searchKey)
                            atom.isMatch = this.matchStr(atomName, searchKey) // 搜索匹配到控制点
                        } else {
                            atom.isDisplay = true
                        }
                        if (!this.isCtrPointPanel) {
                            atom.details.forEach(task => {
                                if (searchKey) {
                                    const taskName = task.detail
                                    task.isDisplay = this.matchStr(taskName, searchKey)
                                    if (task.isDisplay) { // 搜索匹配到指标集展开显示所属控制点
                                        atom.isDisplay = true
                                        task.isDropdownShow = false
                                    }
                                    if (atom.isMatch) {
                                        task.isDropdownShow = false
                                    }
                                } else {
                                    task.isDisplay = true
                                }
                                task.items.forEach(meta => {
                                    meta.isSelected = this.selectedMeta.some(val => meta.hashId === val.hashId)
                                    if (searchKey) {
                                        const metaName = meta.cnName
                                        meta.isDisplay = this.matchStr(metaName, searchKey) || this.matchStr(task.detail, searchKey)
                                        if (meta.isDisplay) { // 搜索匹配到指标展开所属指标集
                                            task.isDisplay = true
                                            task.isDropdownShow = true
                                        }
                                    } else {
                                        meta.isDisplay = true
                                    }
                                })
                            })
                        }
                    })
                })
                
                if (!this.isCtrPointPanel) {
                    // 查找搜索结果父节点展开
                    container.forEach(item => {
                        item.controlPoints.forEach(atom => {
                            if (searchKey && !atom.isDisplay) {
                                atom.isDisplay = atom.details.some(task => task.isDisplay)
                            }
                            atom.details.forEach(task => {
                                task.isSelected = task.items.every(meta => meta.isSelected)
                            })
                        })
                    })
                }
                
                this.metaTree = [...container]
            },
            getIndicatortList (container, searchKey) {
                container.forEach(task => {
                    task.isDropdownShow = false
                    if (searchKey) {
                        const taskName = task.name
                        task.isDisplay = this.matchStr(taskName, searchKey)
                        if (task.isDisplay) { // 搜索匹配到指标集展开显示所属控制点
                            task.isDropdownShow = false
                        }
                    } else {
                        task.isDisplay = true
                    }
                    task.indicators.forEach(meta => {
                        meta.isSelected = this.selectedMeta.some(val => meta.hashId === val.hashId)
                        if (searchKey) {
                            const metaName = meta.cnName
                            meta.isDisplay = this.matchStr(metaName, searchKey)
                            if (meta.isDisplay) { // 搜索匹配到指标展开所属指标集
                                task.isDisplay = true
                                task.isDropdownShow = true
                            }
                        } else {
                            meta.isDisplay = true
                        }
                    })
                })
                
                container.forEach(task => {
                    task.isSelected = task.indicators.every(meta => meta.isSelected)
                })

                this.metaTree = [...container]
            },
            toggleDropdown (taskId) {
                if (this.isIndexList) {
                    this.metaTree.forEach(task => {
                        if (task.hashId === taskId) {
                            task.isDropdownShow = !task.isDropdownShow
                            task.indicators.map(meta => {
                                return {
                                    ...meta,
                                    isDisplay: task.isDropdownShow
                                }
                            })
                        }
                    })
                } else {
                    this.metaTree.forEach(stage => {
                        stage.controlPoints.forEach(atom => {
                            atom.details.forEach(task => {
                                if (task.hashId === taskId) {
                                    task.isDropdownShow = !task.isDropdownShow
                                    task.items.map(meta => {
                                        return {
                                            ...meta,
                                            isDisplay: task.isDropdownShow
                                        }
                                    })
                                }
                            })
                        })
                    })
                }
                this.metaTree = [...this.metaTree]
            },
            getAtomIcon (atomType) {
                return document.getElementById(atomType) ? atomType : 'placeholder'
            },
            getIndicatorDesc (metadataName) {
                const temp = []
                metadataName.forEach(item => {
                    temp.push(item.name)
                })
                return temp.join('+')
            },
            matchStr (target, key) {
                return target && target.toLowerCase().indexOf(key.toLowerCase()) > -1
            },
            changeTab (tab) {
                this.currentTab = tab
                if (tab === 'singleIndex') {
                    this.requestIndicators()
                    // this.getMetaList(this.indicatorList, this.searchKey)
                } else {
                    this.getIndicatortList(this.indicatorSetList, this.searchKey)
                }
            },
            toSearch () {
                this.searchKey = this.searchKey.trim()
                if (this.isCtrPointPanel) {
                    this.getMetaList(this.metaTree, this.searchKey)
                } else if (!this.isCtrPointPanel && !this.isIndexList) {
                    this.getMetaList(this.metaTree, this.searchKey)
                } else {
                    this.getIndicatortList(this.metaTree, this.searchKey)
                }
            },
            selectNode (node, type) {
                const params = {}
                if (type === 'indexList') {
                    params.data = node.indicators
                    params.type = 'metadata'
                } else if (type === 'singleIndex') {
                    params.data = [{ ...node }]
                    params.type = 'metadata'
                } else {
                    params.data = node
                    params.type = 'controlPoint'
                }
                this.$emit('comfireHandle', params)
                if (!this.isCtrPointPanel) {
                    setTimeout(() => {
                        this.isIndexList ? this.getIndicatortList(this.indicatorSetList, this.searchKey) : this.getMetaList(this.indicatorList, this.searchKey)
                    }, 10)
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf.scss';
    .metadata-select-panel {
        position: relative;
        height: 100%;
        .bk-tab-section {
            display: none;
        }
        .metadata-panel-header {
            position: absolute;
            top: -48px;
            right: 20px;
        }
        .search-input-row {
            position: relative;
            padding: 0 10px;
            width: 279px;
            height: 36px;
            border: 1px solid #dde4eb;
            background-color: #fff;
            .bk-form-input {
                padding: 0;
                border: 0;
                -webkit-box-shadow: border-box;
                box-shadow: border-box;
                outline: none;
                width: 239px;
                height: 32px;
                margin-left: 0;
            }
            .icon-search {
                float: right;
                margin-top: 12px;
                color: #c3cdd7;
                cursor: pointer;
            }
        }
        .crtl-point-panel {
            top: 0;
        }
        .bk-tab2 {
            border: none;
            .bk-tab2-head {
                height: 32px;
            }
            .bk-tab2-nav > .tab2-nav-item {
                padding: 0 8px;
                height: 32px;
                line-height: 32px;
                font-size: 14px;
                &:first-child {
                    margin-right: 14px;
                }
            }
        }
        .metadata-panel-container {
            height: 100%;
            padding-bottom: 20px;
            overflow: auto;
        }
        .metadata-main-tab {
            padding: 18px 20px 0 24px;
        }
        .indicator-list-tab {
            padding-bottom: 10px;
        }
        .classify-header {
            position: relative;
            height: 32px;
            line-height: 32px;
            margin-top: 20px;
            padding: 0 24px;
            color: #333C48;
            background-color: #EBEEF3;
            font-weight: bold;
            > .title {
                margin-left: 10px;
            }
            &:before {
                content: '';
                position: absolute;
                top: 9px;
                width: 4px;
                height: 14px;
                background-color: #737987;
            }
        }
        .proprety-item-contnet {
            display: flex;
            justify-content: space-between;
            margin: 10px 24px 0;
            padding: 9px 11px;
            border: 1px solid #DDE4EB;
            font-size: 16px;
            cursor: pointer;
            .info-title {
                display: flex;
                line-height: 22px;
            }
            .devops-icon {
                margin-right: 20px;
                font-size: 24px;
                color: #C3CDD7;
            }
            .atom-name {
                margin-left: 20px;
                color: #333C48;
            }
            span,
            .select-btn {
                position: relative;
                top: 1px;
            }
            .handle-btn {
                position: relative;
                top: 3px;
                right: 9px;
                color: $primaryColor;
                font-size: 14px;
            }
            .select-btn {
                display: none;
                color: $primaryColor;
            }
            .selected-btn {
                color: #979BA5;
            }
        }
        .task-item-content {
            position: relative;
            margin: 10px 24px 0;
            padding: 8px 11px;
            font-size: 14px;
            .dropdown-icon {
                flex: 1;
                text-align: right;
            }
            .hide {
                opacity: 0;
            }
            .icon-angle-double-right {
                display: inline-block;
                position: relative;
                top: 2px;
                margin-right: 0;
                color: #C5C7D1;
                font-size: 12px;
                font-weight: bold;
                &.icon-flip {
                    transform: rotate(90deg);
                }
            }
            .task-name {
                flex: 1.5;
                margin-right: 10px;
                font-weight: bold;
                color: #333C48;
                white-space:nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            .task-desc {
                flex: 5.5;
                color: #979BA5;
                white-space:nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            .handle-btn {
                position: absolute;
                top: 8px;
                right: 45px;
            }
            &.hover {
                background-color: #EBEEF3;
            }
        }
        .metadata-item-content {
            position: relative;
            margin-top: 0;
            padding: 7px 80px 7px 11px;
            border-top: none;
            font-size: 12px;
            .meta-name {
                flex: 2;
                margin-right: 10px;
                font-weight: bold;
                white-space:nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            .meta-desc {
                flex: 4;
                color: #979BA5;
                white-space:nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            .handle-btn {
                position: absolute;
                top: 7px;
                right: 12px;
                font-size: 12px;
            }
        }
        .optional-item {
            &:hover {
                background-color: #EBF4FF;
                color: $primaryColor;
                .atom-name,
                .task-name,
                .task-desc,
                .meta-name,
                .meta-desc {
                    color: $primaryColor;
                }
                .select-btn {
                    display: block;
                }
            }
        }
    }
</style>

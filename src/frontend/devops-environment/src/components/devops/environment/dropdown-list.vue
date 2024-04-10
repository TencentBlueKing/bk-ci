<template>
    <ul class="dropdown-list first-col">
        <li class="host-main" :class="{ 'disabled': !entry.canUse }"
            v-for="(entry, index) in menuList" :key="index"
            @click="handleNode(entry.id, entry.canUse)">
            <span>{{ entry.name }}</span>
            <i class="devops-icon icon-right-shape" v-if="entry.children"></i>
            <ul class="dropdown-list second-col" v-if="entry.children"
                :class="{ 'set-col': entry.id === 'hostConf' }">
                <li v-for="(child, eindex) in entry.children" :key="eindex"
                    :class="{ 'disabled': !child.canUse }"
                    @click="handleNode(child.id, child.canUse)">
                    <span>{{ child.name }}</span>
                </li>
            </ul>
        </li>
    </ul>
</template>

<script>
    export default {
        props: {
            isShow: Boolean,
            node: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                invalidStatus: ['DELETED', 'CREATING', 'STARTING', 'STOPPING', 'RESTARTING', 'DELETING', 'BUILDING_IMAGE'],
                validStatus: ['RUNNING', 'NORMAL']
            }
        },
        computed: {
            menuList () {
                const menuList = [
                    {
                        id: 'hostStatus',
                        name: this.$t('environment.nodeInfo.cpuStatus'),
                        canUse: this.validStatus.includes(this.node.nodeStatus),
                        children: [
                            { id: 'restart', name: this.$t('environment.restart'), canUse: false },
                            { id: 'boot', name: this.$t('environment.boot'), canUse: false },
                            { id: 'shut', name: this.$t('environment.shutDown'), canUse: false },
                            { id: 'destory', name: this.$t('environment.destory'), canUse: !this.invalidStatus.includes(this.node.nodeStatus) }
                        ]
                    },
                    {
                        id: 'hostConf',
                        name: this.$t('environment.hostConfig'),
                        canUse: false,
                        children: [
                            { id: 'modifyConf', name: this.$t('environment.editConfig'), canUse: false },
                            { id: 'hardDisk', name: this.$t('environment.editHardDisk'), canUse: false }
                        ]
                    },
                    { id: 'flowApply', name: this.$t('environment.flowImportApply'), canUse: false },
                    { id: 'makeImage', name: this.$t('environment.makeMirror'), canUse: this.validStatus.includes(this.node.nodeStatus) },
                    { id: 'reinstallOs', name: this.$t('environment.reinstallOs'), canUse: false }
                ]
                return menuList
            }
        },
        methods: {
            handleNode (name, canUse) {
                this.$emit('handleNode', name, canUse, this.node)
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import './../../../scss/conf';
    .dropdown-list {
        position: absolute;
        top: -2px;
        left: -30px;
        width: 94px;
        height: auto;
        border: 1px solid #c3cdd7;
        background: #fff;
        box-shadow: 0 2px 6px rgba(51,60,72,.1);
        z-index: 99;
        li {
            position: relative;
            padding: 10px 8px;
            color: $fontColor;
            font-size: 12px;
            background: #fff;
            cursor: pointer;
        }
        .icon-right-shape {
            display: inline-block;
            position: relative;
            top: 1px;
            left: 4px;
            color: $fontLighterColor;
        }
        li:hover {
            background-color: #ebf4ff;
            color: $primaryColor;
            .icon-right-shape {
                transition: all ease 0.2s;
                transform: rotate(90deg);
            }
        }
        .disabled {
            color: #ccc;
            background-color: #fafafa;
            &:hover {
                color: #ccc;
                background-color: #fafafa;
            }
        }
        .second-col {
            position: absolute;
            top: -1px;
            left: 93px;
            width: 54px;
            display: none;
        }
        .host-main:hover {
            .second-col {
                display: block;
            }
        }
        .set-col {
            width: 90px;
        }
    }
</style>

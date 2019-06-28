<template>
    <div v-bk-clickoutside="toggleCrumbList" :class="{ 'bread-crumb-item': true, 'active': isActive, disabled: !to && hasRecords }">
        <slot :activeName="activeName">
            <i v-if="icon" :class="`bk-icon icon-${icon} bread-crumb-item-icon`" />
            <span @click="handleNameClick" :title="activeName" :disabled="!to" class="bread-crumb-name">{{activeName}}</span>
        </slot>
        <show-tooltip v-if="showTips" placement="bottom" :content="tipsContent" :name="tipsName">
            <span @click.stop="breadCrumbItemClick" :class="{ 'bk-icon': true, 'icon-angle-right': true, 'active': isActive, 'is-cursor': hasRecords }"></span>
        </show-tooltip>
        <span v-else @click.stop="breadCrumbItemClick" :class="{ 'bk-icon': true, 'icon-angle-right': true, 'active': isActive, 'is-cursor': hasRecords }"></span>
        <template v-if="hasRecords">
            <crumb-records v-if="isActive" :param-id="paramId" :param-name="paramName" :records="records" :handle-record-click="handleRecordClick" :active-id="activeId"></crumb-records>
        </template>
    </div>
</template>

<script>
    import CrumbRecords from './CrumbRecords'
    import showTooltip from '@/components/common/showTooltip'

    export default {
        name: 'bread-crumb-item',
        components: {
            CrumbRecords,
            showTooltip
        },
        props: {
            showTips: Boolean,
            tipsName: String,
            tipsContent: String,
            records: {
                type: Array
            },
            record: {
                type: Object
            },
            paramId: {
                type: String,
                default: 'id'
            },
            paramName: {
                type: String,
                default: 'name'
            },
            selectedValue: {
                type: String
            },
            handleSelected: {
                type: Function
            },
            to: Object,
            icon: String
        },
        data () {
            return {
                isActive: false,
                activeId: this.selectedValue
            }
        },
        computed: {
            activeName () {
                if (!Array.isArray(this.records)) {
                    return this.activeId
                }
                const item = this.records.find(r => r[this.paramId] === this.activeId)
                return item && item[this.paramName] ? item[this.paramName] : ''
            },
            hasRecords () {
                return Array.isArray(this.records) && this.records.length
            }
        },
        watch: {
            selectedValue (newVal) {
                this.activeId = newVal
            }
        },
        methods: {
            toggleCrumbList (isShow) {
                if (this.hasRecords) {
                    this.isActive = typeof isShow === 'boolean' ? isShow : false
                }
            },
            breadCrumbItemClick () {
                this.toggleCrumbList(!this.isActive)
            },
            handleRecordClick (record) {
                this.record = record
                this.activeId = record[this.paramId]
                this.toggleCrumbList(false)
                this.handleSelected(this.activeId, record)
            },
            goPrev () {
                this.$router.push(this.to)
            },
            handleNameClick () {
                if (this.to) {
                    this.goPrev()
                } else if (this.hasRecords) {
                    this.breadCrumbItemClick()
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import "../../scss/conf";
    @import "../../scss/mixins/ellipsis";
    .bread-crumb-item {
        display: flex;
        align-items: center;
        height: 32px;
        line-height: 32px;
        border-radius: 10px;

        &.active,
        &.disabled:hover {
            background-color: #f5f5f5;
        }

        &:last-child {
            .icon-angle-right {
                display: none;
            }
        }
        .bread-crumb-name {
            outline: none;
            max-width: 360px;
            cursor: default;
            @include ellipsis();
            &:not([disabled]) {
                cursor: pointer;
                &:hover {
                    color: $primaryColor;
                }
            }
        }
        .bk-icon {
            font-size: 12px;
            margin: 0 8px;
            font-weight: bold;
            display: inline-block;
            &.is-cursor {
                cursor: pointer;
                color: $primaryColor;
                &:hover {
                    color: #3a84ff;
                }
            }
            &.active {
                transform: rotate(90deg);
                transition: all .3s ease;
                color: $primaryColor;
            }
        }
        .bread-crumb-item-icon {
            margin: 0 8px 0 0;
            font-size: 20px;
        }
    }
    .spin-icon {
        display: inline-block;
    }
</style>

<template>
    <div v-bk-clickoutside="toggleCrumbList" :class="{ 'bread-crumb-item': true, 'active': isActive, 'disabled': !to && hasRecords }">
        <slot>
            <i v-if="icon" :class="`devops-icon icon-${icon} bread-crumb-item-icon`" />
            <span @click="handleNameClick" :title="selectedValue" :disabled="!to" class="bread-crumb-name">{{selectedValue}}</span>
        </slot>
        <span @click.stop="breadCrumbItemClick" :class="{ 'devops-icon': true, 'icon-angle-right': true, 'active': isActive, 'is-cursor': hasRecords }"></span>
        <template v-if="hasRecords">
            <crumb-records v-if="isActive" :searching="searching" :param-id="paramId" :param-name="paramName" :records="records" :handle-record-click="handleRecordClick" @searchInput="handleSearch" :selected-value="selectedValue"></crumb-records>
        </template>
    </div>
</template>

<script>
    import CrumbRecords from './CrumbRecords'

    export default {
        name: 'bread-crumb-item',
        components: {
            CrumbRecords
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
            handleSearch: {
                type: Function
            },
            searching: Boolean,
            to: Object,
            icon: String
        },
        data () {
            return {
                isActive: false
            }
        },
        computed: {
            hasRecords () {
                return Array.isArray(this.records)
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
                this.toggleCrumbList(false)
                this.handleSelected(record[this.paramId], record)
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
        .devops-icon {
            font-size: 12px;
            margin: 0 8px;
            display: inline-block;
            color: #C3CDD7;
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
            font-weight: bold;
            color: #63656E;
        }
    }
    .spin-icon {
        display: inline-block;
    }
</style>

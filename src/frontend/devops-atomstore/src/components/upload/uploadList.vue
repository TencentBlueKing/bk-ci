<template>
    <transition-group
        tag="ul"
        :class="[
            'el-upload-list',
            'el-upload-list--' + listType,
            { 'is-disabled': disabled }
        ]"
        name="fade"
    >
        <li
            v-for="file in files"
            :class="['el-upload-list__item', 'is-' + file.status, focusing ? 'focusing' : '']"
            :key="file.uid"
            tabindex="0"
            @keydown.delete="!disabled && $emit('remove', file)"
            @focus="() => (focusing = true)"
            @blur="() => (focusing = false)"
            @click="() => (focusing = false)"
        >
            <slot :file="file"></slot>
        </li>
    </transition-group>
</template>
<script>
    export default {
        props: {
            files: {
                type: Array,
                default () {
                    return []
                }
            },
            disabled: {
                type: Boolean,
                default: false
            },
            handlePreview: Function,
            listType: String
        },

        data () {
            return {
                focusing: false
            }
        },
        methods: {
            parsePercentage (val) {
                return parseInt(val, 10)
            },
            handleClick (file) {
                this.handlePreview && this.handlePreview(file)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .el-upload-list {
        margin: 0;
        padding: 0;
        list-style: none
    }

    .el-upload-list__item {
        transition: all .5s cubic-bezier(.55,0,.1,1);
        font-size: 14px;
        color: #606266;
        line-height: 1.8;
        margin-top: 5px;
        position: relative;
        box-sizing: border-box;
        border-radius: 4px;
        width: 100%
    }

    .el-upload-list__item .el-progress {
        position: absolute;
        top: 20px;
        width: 100%
    }

    .el-upload-list__item .el-progress__text {
        position: absolute;
        right: 0;
        top: -13px
    }

    .el-upload-list__item .el-progress-bar {
        margin-right: 0;
        padding-right: 0
    }

    .el-upload-list__item:first-child {
        margin-top: 10px
    }

    .el-upload-list__item .el-icon-upload-success {
        color: #67c23a
    }

    .el-upload-list__item .el-icon-close {
        display: none;
        position: absolute;
        top: 5px;
        right: 5px;
        cursor: pointer;
        opacity: .75;
        color: #606266
    }

    .el-upload-list__item .el-icon-close:hover {
        opacity: 1
    }

    .el-upload-list__item .el-icon-close-tip {
        display: none;
        position: absolute;
        top: 5px;
        right: 5px;
        font-size: 12px;
        cursor: pointer;
        opacity: 1;
        color: #409eff
    }

    .el-upload-list__item:hover {
        background-color: #f5f7fa
    }

    .el-upload-list__item:hover .el-icon-close {
        display: inline-block
    }

    .el-upload-list__item:hover .el-progress__text {
        display: none
    }

    .el-upload-list__item.is-success .el-upload-list__item-status-label {
        display: block
    }

    .el-upload-list__item.is-success .el-upload-list__item-name:focus,.el-upload-list__item.is-success .el-upload-list__item-name:hover {
        color: #409eff;
        cursor: pointer
    }

    .el-upload-list__item.is-success:focus:not(:hover) .el-icon-close-tip {
        display: inline-block
    }

    .el-upload-list__item.is-success:active,.el-upload-list__item.is-success:not(.focusing):focus {
        outline-width: 0
    }

    .el-upload-list__item.is-success:active .el-icon-close-tip,.el-upload-list__item.is-success:focus .el-upload-list__item-status-label,.el-upload-list__item.is-success:hover .el-upload-list__item-status-label,.el-upload-list__item.is-success:not(.focusing):focus .el-icon-close-tip {
        display: none
    }

    .el-upload-list.is-disabled .el-upload-list__item:hover .el-upload-list__item-status-label {
        display: block
    }

    .el-upload-list__item-name {
        color: #606266;
        display: block;
        margin-right: 40px;
        overflow: hidden;
        padding-left: 4px;
        text-overflow: ellipsis;
        transition: color .3s;
        white-space: nowrap
    }

    .el-upload-list__item-name [class^=el-icon] {
        height: 100%;
        margin-right: 7px;
        color: #909399;
        line-height: inherit
    }

    .el-upload-list__item-status-label {
        position: absolute;
        right: 5px;
        top: 0;
        line-height: inherit;
        display: none
    }

    .el-upload-list__item-delete {
        position: absolute;
        right: 10px;
        top: 0;
        font-size: 12px;
        color: #606266;
        display: none
    }

    .el-upload-list__item-delete:hover {
        color: #409eff
    }

    .el-upload-list--picture-card {
        margin: 0;
        display: inline;
        vertical-align: top
    }

    .el-upload-list--picture-card .el-upload-list__item {
        overflow: hidden;
        background-color: #fff;
        border: 1px solid #c0ccda;
        border-radius: 6px;
        box-sizing: border-box;
        width: 148px;
        height: 148px;
        margin: 0 8px 8px 0;
        display: inline-block
    }

    .el-upload-list--picture-card .el-upload-list__item .el-icon-check,.el-upload-list--picture-card .el-upload-list__item .el-icon-circle-check {
        color: #fff
    }

    .el-upload-list--picture-card .el-upload-list__item .el-icon-close,.el-upload-list--picture-card .el-upload-list__item:hover .el-upload-list__item-status-label {
        display: none
    }

    .el-upload-list--picture-card .el-upload-list__item:hover .el-progress__text {
        display: block
    }

    .el-upload-list--picture-card .el-upload-list__item-name {
        display: none
    }

    .el-upload-list--picture-card .el-upload-list__item-thumbnail {
        width: 100%;
        height: 100%
    }

    .el-upload-list--picture-card .el-upload-list__item-status-label {
        position: absolute;
        right: -15px;
        top: -6px;
        width: 40px;
        height: 24px;
        background: #13ce66;
        text-align: center;
        transform: rotate(45deg);
        box-shadow: 0 0 1pc 1px rgba(0,0,0,.2)
    }

    .el-upload-list--picture-card .el-upload-list__item-status-label i {
        font-size: 12px;
        margin-top: 11px;
        transform: rotate(-45deg)
    }

    .el-upload-list--picture-card .el-upload-list__item-actions {
        position: absolute;
        width: 100%;
        height: 100%;
        left: 0;
        top: 0;
        cursor: default;
        text-align: center;
        color: #fff;
        opacity: 0;
        font-size: 20px;
        background-color: rgba(0,0,0,.5);
        transition: opacity .3s
    }

    .el-upload-list--picture-card .el-upload-list__item-actions:after {
        display: inline-block;
        content: "";
        height: 100%;
        vertical-align: middle
    }

    .el-upload-list--picture-card .el-upload-list__item-actions span {
        display: none;
        cursor: pointer
    }

    .el-upload-list--picture-card .el-upload-list__item-actions span+span {
        margin-left: 15px
    }

    .el-upload-list--picture-card .el-upload-list__item-actions .el-upload-list__item-delete {
        position: static;
        font-size: inherit;
        color: inherit
    }

    .el-upload-list--picture-card .el-upload-list__item-actions:hover {
        opacity: 1
    }

    .el-upload-list--picture-card .el-upload-list__item-actions:hover span {
        display: inline-block
    }

    .el-upload-list--picture-card .el-progress {
        top: 50%;
        left: 50%;
        transform: translate(-50%,-50%);
        bottom: auto;
        width: 126px
    }

    .el-upload-list--picture-card .el-progress .el-progress__text {
        top: 50%
    }

    .el-upload-list--picture .el-upload-list__item {
        overflow: hidden;
        z-index: 0;
        background-color: #fff;
        border: 1px solid #c0ccda;
        border-radius: 6px;
        box-sizing: border-box;
        margin-top: 10px;
        padding: 10px 10px 10px 90px;
        height: 92px
    }

    .el-upload-list--picture .el-upload-list__item .el-icon-check,.el-upload-list--picture .el-upload-list__item .el-icon-circle-check {
        color: #fff
    }

    .el-upload-list--picture .el-upload-list__item:hover .el-upload-list__item-status-label {
        background: transparent;
        box-shadow: none;
        top: -2px;
        right: -12px
    }

    .el-upload-list--picture .el-upload-list__item:hover .el-progress__text {
        display: block
    }

    .el-upload-list--picture .el-upload-list__item.is-success .el-upload-list__item-name {
        line-height: 70px;
        margin-top: 0
    }

    .el-upload-list--picture .el-upload-list__item.is-success .el-upload-list__item-name i {
        display: none
    }

    .el-upload-list--picture .el-upload-list__item-thumbnail {
        vertical-align: middle;
        display: inline-block;
        width: 70px;
        height: 70px;
        float: left;
        position: relative;
        z-index: 1;
        margin-left: -80px;
        background-color: #fff
    }

    .el-upload-list--picture .el-upload-list__item-name {
        display: block;
        margin-top: 20px
    }

    .el-upload-list--picture .el-upload-list__item-name i {
        font-size: 70px;
        line-height: 1;
        position: absolute;
        left: 9px;
        top: 10px
    }

    .el-upload-list--picture .el-upload-list__item-status-label {
        position: absolute;
        right: -17px;
        top: -7px;
        width: 46px;
        height: 26px;
        background: #13ce66;
        text-align: center;
        transform: rotate(45deg);
        box-shadow: 0 1px 1px #ccc
    }

    .el-upload-list--picture .el-upload-list__item-status-label i {
        font-size: 12px;
        margin-top: 12px;
        transform: rotate(-45deg)
    }

    .el-upload-list--picture .el-progress {
        position: relative;
        top: -7px
    }
</style>

<template>
    <div class="consult-tools">
        <a
            v-for="(entry, index) in consultTypeList"
            :key="index"
            :class="{ 'consult-item': true, 'default-hover': index === 0 }"
            :href="entry.href"
            :target="entry.target"
        >
            <icon
                class="bk-icon"
                :name="entry.icon"
                size="13"
            />
            <div
                class="custom-panel trans-fadeout"
                @click.stop="prevent($event)"
            >
                <div class="panel-arrow" />
                <p>{{ entry.label }}</p>
            </div>
        </a>
    </div>
</template>

<script lang='ts'>
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'

    @Component
    export default class ConsultTools extends Vue {
        get consultTypeList () {
            return [
                {
                    icon: 'telephone',
                    label: this.$t('hotline') + '56000',
                    href: '#',
                    target: ''
                },
                {
                    icon: 'service',
                    label: this.$t('contactUs'),
                    href: 'wxwork://message/?username=DevOps',
                    target: ''
                },
                {
                    icon: 'feedback',
                    label: this.$t('feedback'),
                    href: 'http://tapd.oa.com/PaaS/prong/stories/stories_list',
                    target: '_blank'
                }
            ]
        }

        prevent (event) {
            event.preventDefault()
        }
    }
</script>

<style lang="scss" scoped>
@import "../../assets/scss/conf";
.consult-tools {
    position: fixed;
    right: 12px;
    top: 66%;
    .custom-panel {
        position: absolute;
        top: -6px;
        right: 30px;
        padding: 8px 10px;
        white-space: nowrap;
        background: rgba(255, 255, 255, 1);
        box-shadow: 0px 2px 4px 0px rgba(195, 205, 215, 0.4);
        border: 1px solid #c3cdd7;
        visibility: hidden;
        opacity: 0;
        cursor: default;
        p {
            color: #737987;
            font-size: 14px;
        }
        .panel-arrow {
            padding-top: 4px;
            position: absolute;
            top: 13px;
            right: -4px;
            width: 8px;
            height: 8px;
            border: 1px solid #c3cdd7;
            border-right-color: transparent;
            border-bottom-color: transparent;
            background-color: #fff;
            transform: rotate(135deg);
        }
    }
    .trans-fadeout {
        -webkit-transition: all 0.1s linear;
        -moz-transition: all 0.1s linear;
        -ms-transition: all 0.1s linear;
        -o-transition: all 0.1s linear;
        transition: all 0.1s linear;
    }
    .consult-item {
        display: inherit;
        position: relative;
        margin-bottom: 5px;
        width: 24px;
        height: 24px;
        text-align: center;
        background: rgba(51, 60, 72, 0.9);
        box-shadow: 0px 2px 4px 0px rgba(115, 121, 135, 0.3);
        border-radius: 2px;
        cursor: pointer;
        color: #fff;
        &:hover {
            background: rgba(51, 60, 72, 0.8);
            .custom-panel {
                visibility: visible;
                opacity: 1;
            }
        }
        .bk-icon {
            display: inline-block;
            margin-top: 6px;
            color: #fff;
            font-size: 13px;
        }
    }
    .default-hover:hover {
        cursor: default;
    }
}
</style>

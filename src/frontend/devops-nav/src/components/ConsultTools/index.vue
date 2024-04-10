<template>
    <div class="consult-tools">
        <a
            v-for="(entry, index) in consultTypeList"
            :key="index"
            class="consult-item"
            :href="entry.href"
            :id="entry.id"
            :target="entry.target"
        >
            <icon
                class="devops-icon"
                :name="entry.icon"
                size="13"
            />
            <p v-if="entry.id === 'contactUs'">
                O2000
                <br />
            </p>
            <p>{{ entry.label }}</p>
        </a>
    </div>
</template>

<script lang='ts'>
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { State } from 'vuex-class'

    @Component
    export default class ConsultTools extends Vue {
        @State user
        get consultTypeList () {
            return [
                {
                    icon: 'customer',
                    label: this.$t('contactUs'),
                    href: 'wxwork://message?uin=8444250473321980',
                    id: 'contactUs',
                    target: ''
                },
                {
                    icon: 'help',
                    label: this.$t('documentation'),
                    href: this.BKCI_DOCS.BKCI_DOC,
                    id: 'documentation',
                    target: '_blank'
                },
                {
                    icon: 'feedback',
                    id: 'feedback',
                    label: this.$t('feedback'),
                    href: this.BKCI_DOCS.FEED_BACK_URL,
                    target: '_blank'
                }
            ]
        }
    }
</script>

<style lang="scss" scoped>
@import "../../assets/scss/conf";
.consult-tools {
    position: fixed;
    right: 1%;
    top: 60%;
    border-radius: 4px;
    border: 1px solid #e3e6eb;
    background: #ffffff;
    box-shadow: 0 4px 20px #5576a21a;
    color: #0009;
    font-size: 12px;
    z-index: 15;
    .consult-item {
        display: inherit;
        text-align: center;
        border-radius: 2px;
        padding: 20px 12px;
        cursor: pointer;
        border-bottom: 1px solid rgba(227, 230, 235, 1);
        &:last-child {
            border-bottom: none;
        }
        .devops-icon {
            display: inline-block;
            width: 24px;
            height: 24px;
            color: #fff;
            font-size: 13px;
        }
    }
}
</style>

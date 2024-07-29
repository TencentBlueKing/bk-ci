<template>
    <div>
        <div class="consult-tools">
            <a
                v-for="(entry, index) in consultTypeList"
                :key="index"
                class="consult-item"
                :href="entry.href"
                :id="entry.id"
                :target="entry.target"
                @click="handleClickConsult(entry)"
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
                <div
                    class="dot"
                    v-if="entry.showReminderDot"
                ></div>
            </a>
        </div>
        <div
            :class="{
                'fixation-wrap': true,
                'fixation-wrap-active': showTabAssistant
            }"
            v-bk-clickoutside="handleHideTabAssistant"
        >
            <iframe
                class="help-center-iframe"
                src="https://yst.woa.com/chat/chatComp?web_key=1712831968_6109_v_minghteng"
                frameborder="0"
            ></iframe>
        </div>
    </div>
</template>

<script lang='ts'>
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { State } from 'vuex-class'
    const SHOW_O2000_REMINDER_DOT = 'SHOW_O2000_REMINDER_DOT'

    @Component
    export default class ConsultTools extends Vue {
        @State user
        showTabAssistant: boolean = false
        
        get consultTypeList () {
            return [
                {
                    icon: 'customer',
                    label: this.$t('contactUs'),
                    // href: 'wxwork://message?uin=8444250473321980',
                    id: 'contactUs',
                    target: '',
                    showReminderDot: !localStorage.getItem(SHOW_O2000_REMINDER_DOT)
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

        handleClickConsult (entry) {
            if (entry.id === 'contactUs') {
                entry.showReminderDot = false
                localStorage.setItem(SHOW_O2000_REMINDER_DOT, 'true')
                this.showTabAssistant = true
            }
        }

        handleHideTabAssistant () {
            if (this.showTabAssistant) this.showTabAssistant = false
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
        .dot {
            position: absolute;
            top: 20px;
            right: 15px;
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: red;
            transform: translate(50%, -50%);
        }
    }
}
.fixation-wrap {
    position: fixed;
    right: 120px;
    bottom: 104px;
    width: 615px;
    height: 640px;
    pointer-events: none;
    display: none;
    z-index: 2000;
    background-color: #ecf2fe;
    border: 1px solid rgba(59, 66, 75, .24);
}
.fixation-wrap-active {
    display: block;
    pointer-events: auto;
}
.help-center-iframe {
    position: fixed;
    right: 120px;
    bottom: 104px;
    width: 615px;
    height: 640px;
    pointer-events: auto;
    z-index: 2000;
    background-color: #ecf2fe;
    border: 1px solid rgba(59, 66, 75, .24);
}
</style>

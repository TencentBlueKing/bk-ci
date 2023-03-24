import {
    defineComponent,
    version
} from '../../vue';

export default defineComponent({
    props: {
        loading: {
            type: Boolean
        },
        isShow: {
            type: String
        },
        title: {
            type: String
        },
        width: {
            type: Number
        },
        theme: {
            type: String
        },
        dialogType: {
            type: String
        },
        quickClose: {
            type: String
        },
        extCls: {
            type: String
        },
        headerAlign: {
            type: String
        }
    },

    emits: ['confirm', 'cancel'],

    setup (_, { emit }) {
        const emitEvent = (name) => {
            emit(name)
        }

        return {
            emitEvent
        }
    },

    render () {
        const { $slots, emitEvent } = this;
        const listeners = {
            confirm () {
                emitEvent('confirm')
            },
            cancel () {
                emitEvent('cancel')
            }
        }
        if (version === 2) {
            return <bk-dialog
                value={this.isShow}
                title={this.title}
                loading={this.loading}
                width={this.width}
                scopedSlots={{
                    default: () => $slots.default,
                    footer: () => $slots.footer,
                }}
                {...{on: listeners }}
            />
        }
        return <bk-dialog
            dialogType={this.dialogType}
            isShow={this.isShow}
            title={this.title}
            loading={this.loading}
            width={this.width}
            theme={this.theme}
            quickClose={this.quickClose}
            extCls={this.extCls}
            headerAlign={this.headerAlign}
            {...{on: listeners }}
        >
            {{
                default: () => $slots.default?.(),
                header: () => $slots.header?.(),
                footer: () => $slots.footer?.()
            }}
        </bk-dialog>
    }
});


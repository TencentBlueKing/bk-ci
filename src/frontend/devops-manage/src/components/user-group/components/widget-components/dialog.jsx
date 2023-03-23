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
            isShow={this.isShow}
            title={this.title}
            loading={this.loading}
            width={this.width}
            {...{on: listeners }}
        >
            {{
                default: () => $slots.default?.(),
                footer: () => $slots.footer?.()
            }}
        </bk-dialog>
    }
});


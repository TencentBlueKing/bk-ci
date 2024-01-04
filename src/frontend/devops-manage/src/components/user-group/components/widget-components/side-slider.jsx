import {
    defineComponent,
    version
} from '../../vue';

export default defineComponent({
    props: {
        width: {
            type: Number
        },
        isShow: {
            type: String
        }
    },

    emits: ['hidden'],

    setup (_, { emit }) {
        const handleHidden = () => {
            emit('hidden')
        }

        return {
            handleHidden
        }
    },

    render () {
        const { $slots, handleHidden } = this;
        if (version === 2) {
            const listeners = {
                'update:isShow'() {
                    handleHidden()
                }
            }
            return <bk-sideslider
                quick-close={true}
                isShow={this.isShow}
                width={this.width}
                scopedSlots={{
                    content: () => $slots.content,
                    header: () => $slots.header,
                    footer: () => $slots.footer
                }}
                {...{on: listeners }}
            />
        }
        return <bk-sideslider
            quick-close
            isShow={this.isShow}
            width={this.width}
            onHidden={() => handleHidden()}
        >
            {{
                default () {
                    $slots.default?.()
                },
                header() {
                    $slots.header?.()
                },
                footer() {
                    $slots.footer?.()
                }
            }}
        </bk-sideslider>
    }
});


import Node from './Node'

export const parsetext = (expression) => {
    const stack = []
    const rangReg = /-/
    const repeatReg = /\//
    const atoms = (`${expression}`).trim().split(',')
    let index = -1
    // eslint-disable-next-line no-plusplus
    while (++index < atoms.length) {
        const enumValue = atoms[index]
        if (rangReg.test(enumValue) && repeatReg.test(enumValue)) {
            // 在指定区间重复
            const [rang, repeatInterval] = enumValue.split('/')
            const [min, max] = rang.split('-')
            stack.push(new Node({
                type: Node.TYPE_RANG_REPEAT,
                min,
                max,
                repeatInterval
            }))
            continue
        } else if (repeatReg.test(enumValue)) {
            // 从指定起始位置重复
            const [value, repeatInterval] = enumValue.split('/')
            stack.push(new Node({
                type: Node.TYPE_REPEAT,
                value,
                repeatInterval
            }))
            continue
        } else if (rangReg.test(enumValue)) {
            // 指定区间
            const [min, max] = enumValue.split('-')
            stack.push(new Node({
                type: Node.TYPE_RANG,
                min,
                max
            }))
            continue
        } else {
            stack.push(new Node({
                type: Node.TYPE_ENUM,
                value: enumValue
            }))
        }
    }
    return stack
}

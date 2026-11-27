// Regression test for #13826: the "移除" button in the template list must call
// the component's delete method instead of evaluating the JS `delete` operator.
const test = require('node:test')
const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')
const { parseComponent, compileTemplate } = require('vue/compiler-sfc')

const sfcPath = path.join(__dirname, '..', 'src', 'views', 'workList', 'template.vue')
const descriptor = parseComponent(fs.readFileSync(sfcPath, 'utf8'))

test('template list remove button does not use the JS delete operator', () => {
    const { code, errors } = compileTemplate({
        source: descriptor.template.content,
        filename: sfcPath,
        id: 'template-list'
    })
    assert.deepStrictEqual(errors, [])
    const deleteOperator = code.match(/delete\s+props\.row/)
    assert.ok(
        !deleteOperator,
        `remove handler compiled to the JS delete operator: "${deleteOperator && deleteOperator[0]}"`
    )
    assert.ok(
        /_vm\.deleteHandle\(props\.row\)/.test(code),
        'expected the remove click handler to call _vm.deleteHandle(props.row)'
    )
})

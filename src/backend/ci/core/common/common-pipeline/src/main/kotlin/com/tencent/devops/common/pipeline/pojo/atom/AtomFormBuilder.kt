package com.tencent.devops.common.pipeline.pojo.atom

import com.tencent.devops.common.pipeline.pojo.atom.form.AtomForm
import com.tencent.devops.common.pipeline.pojo.atom.form.AtomFromExecution
import com.tencent.devops.common.pipeline.pojo.atom.form.AtomFromInputGroups
import com.tencent.devops.common.pipeline.pojo.atom.form.AtomFromOutputItem

class AtomFormBuilder {
    private val form = AtomForm(atomCode = "", input = mapOf())

    fun atomCode(atomCode: String): AtomFormBuilder {
        form.atomCode = atomCode
        return this
    }

    fun input(input: Map<String, Any>): AtomFormBuilder {
        form.input = input
        return this
    }

    fun inputGroup(inputGroup: List<AtomFromInputGroups>): AtomFormBuilder {
        form.inputGroup = inputGroup
        return this
    }

    fun execution(execution: AtomFromExecution): AtomFormBuilder {
        form.execution = execution
        return this
    }

    fun output(output: Map<String, AtomFromOutputItem>): AtomFormBuilder {
        form.output = output
        return this
    }

    fun build(): AtomForm {
        return form
    }
}
import type { AtomModal } from '@/api/atom'
import type { AdditionalOptions, Container, CustomVariable, Element, FlowSettings } from '@/types/flow'
import { RunLockType } from '@/types/flow'
import { AtomRunCondition, JobRunCondition } from './flowDefaults'
import { rely } from './atom'

function isValueEmpty(value: unknown): boolean {
  if (value === undefined || value === null || value === '') return true
  if (Array.isArray(value) && value.length === 0) return true
  return false
}

function hasEmptyCustomVariable(variables: CustomVariable[]): boolean {
  if (!variables || variables.length === 0) return true
  return variables.some((v) => !v.key?.trim())
}

/**
 * Validate additionalOptions fields of an atom element.
 * Only validates when the plugin is enabled.
 */
export function validateAdditionalOptions(options?: AdditionalOptions): string[] {
  if (!options || options.enable === false) return []

  const errors: string[] = []

  if (isValueEmpty(options.timeoutVar)) {
    errors.push('timeoutVar')
  }

  const { runCondition } = options

  if (runCondition === AtomRunCondition.CUSTOM_CONDITION_MATCH) {
    if (isValueEmpty(options.customCondition)) {
      errors.push('customCondition')
    }
  }

  if (
    runCondition === AtomRunCondition.CUSTOM_VARIABLE_MATCH ||
    runCondition === AtomRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
  ) {
    if (hasEmptyCustomVariable(options.customVariables)) {
      errors.push('customVariables')
    }
  }

  return errors
}

/**
 * Validate an atom element's required fields against its atom modal definition,
 * including additionalOptions validation.
 * Returns a list of field keys that failed validation (empty = valid).
 */
export function validateAtomElement(
  element: Element,
  atomModal: AtomModal | null,
  atomValue: Record<string, unknown>,
): string[] {
  const atomCode = element.atomCode || element['@type']
  if (!atomCode) return ['noAtomSelected']

  const errors: string[] = []

  // Validate plugin input fields
  if (atomModal?.props) {
    const inputProps = (atomModal.props.input as Record<string, any>) || atomModal.props
    if (inputProps && typeof inputProps === 'object') {
      for (const [key, prop] of Object.entries(inputProps)) {
        if (!prop || typeof prop !== 'object') continue
        if (prop.hidden === true) continue

        try {
          if (!rely(prop, atomValue)) continue
        } catch {
          continue
        }

        if (prop.required && isValueEmpty(atomValue[key])) {
          errors.push(key)
        }
      }
    }
  }

  // Validate additionalOptions
  errors.push(...validateAdditionalOptions(element.additionalOptions))

  return errors
}

/**
 * Validate jobControlOption fields of a container.
 * Only validates when the job is enabled.
 */
export function validateJobControlOption(container: Container): string[] {
  const ctrl = container.jobControlOption
  if (!ctrl || ctrl.enable === false) return []

  const errors: string[] = []

  if (isValueEmpty(ctrl.timeout) && isValueEmpty(ctrl.timeoutVar)) {
    errors.push('jobTimeout')
  }

  const { runCondition } = ctrl

  if (runCondition === JobRunCondition.CUSTOM_CONDITION_MATCH) {
    if (isValueEmpty(ctrl.customCondition)) {
      errors.push('jobCustomCondition')
    }
  }

  if (
    runCondition === JobRunCondition.CUSTOM_VARIABLE_MATCH ||
    runCondition === JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
  ) {
    if (hasEmptyCustomVariable(ctrl.customVariables)) {
      errors.push('jobCustomVariables')
    }
  }

  return errors
}

/**
 * Validate a job container's own required fields,
 * including jobControlOption and mutexGroup validation.
 * Returns a list of field keys that failed validation (empty = valid).
 */
export function validateContainer(container: Container): string[] {
  const errors: string[] = []

  if (!container.jobId?.trim()) {
    errors.push('jobId')
  }

  if (container.mutexGroup?.enable && !container.mutexGroup?.mutexGroupName?.trim()) {
    errors.push('mutexGroupName')
  }

  errors.push(...validateJobControlOption(container))

  return errors
}

/**
 * Check if a container should be marked as error based on its own fields only.
 * Does not consider child element errors.
 */
export function computeContainerIsError(container: Container): boolean {
  return validateContainer(container).length > 0
}

/**
 * Validate FlowSettings required fields.
 * Returns a list of field keys that failed validation (empty = valid).
 */
export function validateFlowSettings(settings: FlowSettings | null): string[] {
  if (!settings) return []

  const errors: string[] = []

  if (!settings.pipelineName?.trim()) {
    errors.push('pipelineName')
  }

  if (settings.runLockType === RunLockType.GROUP_LOCK) {
    if (!settings.concurrencyGroup?.trim()) {
      errors.push('concurrencyGroup')
    }
  }

  return errors
}

/**
 * Strip `isError` from all elements and containers in the model before saving.
 */
export function stripIsErrorFromModel<T>(model: T): T {
  const copy = JSON.parse(JSON.stringify(model))
  if (copy?.stages) {
    for (const stage of copy.stages) {
      delete stage.isError
      if (stage.containers) {
        for (const container of stage.containers) {
          delete container.isError
          if (container.elements) {
            for (const element of container.elements) {
              delete element.isError
            }
          }
        }
      }
    }
  }
  return copy
}

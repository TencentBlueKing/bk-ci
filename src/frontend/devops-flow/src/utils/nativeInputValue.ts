/**
 * Read the current DOM value from a native input/textarea event.
 * bkui-vue Input swallows `input`/`change` while IME composition is active,
 * so callers should commit this value on blur.
 */
export function readNativeInputValue(e: Event): string | undefined {
  const target = e.target
  if (target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement) {
    return target.value
  }
  return undefined
}

/**
 * Copy the native input value onto a form field.
 * Returns the applied value, or undefined when the event has no readable input.
 */
export function applyNativeInputValue<T extends object>(
  form: T,
  field: keyof T,
  e: Event,
): string | undefined {
  const nativeValue = readNativeInputValue(e)
  if (nativeValue === undefined) return undefined
  ;(form as Record<PropertyKey, unknown>)[field as PropertyKey] = nativeValue
  return nativeValue
}

export function unstable_cache<T>(fn: T): T {
  return fn
}

export function revalidateTag(_tag: string): void {}

export function revalidatePath(_path: string): void {}

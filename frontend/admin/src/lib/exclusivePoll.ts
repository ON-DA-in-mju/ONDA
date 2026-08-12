/** 이전 요청이 끝나기 전에 폴링이 겹치지 않게 하는 헬퍼 */
export function createExclusivePoll(fn: () => Promise<void>) {
  let inFlight = false
  return async () => {
    if (inFlight) return
    inFlight = true
    try {
      await fn()
    } finally {
      inFlight = false
    }
  }
}

const DEFAULT_PAGE_WINDOW = 10

export function listPageCount(total: number, pageSize: number): number {
  return Math.max(1, Math.ceil(Math.max(0, total) / Math.max(1, pageSize)))
}

export function listPageRangeLabel(total: number, page: number, pageSize: number): string {
  if (total <= 0) return '0-0'
  const safePage = Math.min(Math.max(1, page), listPageCount(total, pageSize))
  const start = (safePage - 1) * pageSize + 1
  const end = Math.min(safePage * pageSize, total)
  return `${start}-${end}`
}

/** 페이지 번호 버튼에 한 번에 보이는 개수(기본 10). 초과 시 현재 페이지 기준 슬라이딩. */
export function visiblePageNumbers(
  page: number,
  pageCount: number,
  windowSize: number = DEFAULT_PAGE_WINDOW,
): number[] {
  if (pageCount <= windowSize) {
    return Array.from({ length: pageCount }, (_, i) => i + 1)
  }
  let start = Math.max(1, page - Math.floor(windowSize / 2))
  let end = start + windowSize - 1
  if (end > pageCount) {
    end = pageCount
    start = end - windowSize + 1
  }
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
}

type ListPaginationProps = {
  total: number
  page: number
  pageSize: number
  onPageChange: (page: number) => void
  ariaLabel?: string
  windowSize?: number
  /** total이 0이면 숨김 (기본 true) */
  hideWhenEmpty?: boolean
}

export function ListPagination({
  total,
  page,
  pageSize,
  onPageChange,
  ariaLabel = '목록 페이지',
  windowSize = DEFAULT_PAGE_WINDOW,
  hideWhenEmpty = true,
}: ListPaginationProps) {
  if (hideWhenEmpty && total <= 0) return null

  const pageCount = listPageCount(total, pageSize)
  const safePage = Math.min(Math.max(1, page), pageCount)
  const pages = visiblePageNumbers(safePage, pageCount, windowSize)
  const range = listPageRangeLabel(total, safePage, pageSize)

  return (
    <div className="list-pagination">
      <div className="list-pagination-meta">
        총 {total.toLocaleString('ko-KR')}건 · {range}
      </div>
      <div className="list-pagination-pages" role="navigation" aria-label={ariaLabel}>
        <button
          type="button"
          className="page-chip"
          disabled={safePage <= 1}
          onClick={() => onPageChange(1)}
          aria-label="첫 페이지"
        >
          «
        </button>
        <button
          type="button"
          className="page-chip"
          disabled={safePage <= 1}
          onClick={() => onPageChange(Math.max(1, safePage - 1))}
          aria-label="이전 페이지"
        >
          ‹
        </button>
        {pages.map((n) => (
          <button
            key={n}
            type="button"
            className={`page-chip${n === safePage ? ' active' : ''}`}
            onClick={() => onPageChange(n)}
            aria-current={n === safePage ? 'page' : undefined}
          >
            {n}
          </button>
        ))}
        <button
          type="button"
          className="page-chip"
          disabled={safePage >= pageCount}
          onClick={() => onPageChange(Math.min(pageCount, safePage + 1))}
          aria-label="다음 페이지"
        >
          ›
        </button>
        <button
          type="button"
          className="page-chip"
          disabled={safePage >= pageCount}
          onClick={() => onPageChange(pageCount)}
          aria-label="마지막 페이지"
        >
          »
        </button>
      </div>
    </div>
  )
}

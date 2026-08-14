const ALLOWED_TAGS = new Set([
  'DIV',
  'P',
  'BR',
  'B',
  'STRONG',
  'I',
  'EM',
  'U',
  'UL',
  'OL',
  'LI',
  'SPAN',
  'IMG',
  'A',
  'H3',
  'H4',
])

export function looksLikeHtml(value: string): boolean {
  return /<\/?[a-z][\s\S]*>/i.test(value)
}

export type NoticeAttachment = { url: string; name: string }

const ATTACH_CLASS = 'onda-notice-attach'
const STORAGE_HINT = 'notice-attachments'

export function splitNoticeAttachments(html: string): { html: string; attachments: NoticeAttachment[] } {
  if (!html.trim()) return { html: '', attachments: [] }
  const doc = new DOMParser().parseFromString(`<div id="onda-root">${html}</div>`, 'text/html')
  const root = doc.getElementById('onda-root')
  if (!root) return { html, attachments: [] }
  const attachments: NoticeAttachment[] = []
  const seen = new Set<string>()

  const add = (url: string, name: string) => {
    const href = url.trim()
    if (!href || seen.has(href)) return
    seen.add(href)
    attachments.push({ url: href, name: name.trim() || fileNameFromUrl(href) })
  }

  for (const el of Array.from(root.querySelectorAll(`.${ATTACH_CLASS}`))) {
    const a = el.querySelector('a[href]')
    const img = el.querySelector('img[src]')
    if (a) add(a.getAttribute('href') ?? '', a.textContent ?? '')
    else if (img) add(img.getAttribute('src') ?? '', img.getAttribute('alt') ?? '')
    el.remove()
  }
  for (const a of Array.from(root.querySelectorAll('a[href]'))) {
    const href = a.getAttribute('href') ?? ''
    if (href.includes(STORAGE_HINT)) {
      add(href, a.textContent ?? '')
      a.remove()
    }
  }
  for (const img of Array.from(root.querySelectorAll('img[src]'))) {
    const src = img.getAttribute('src') ?? ''
    if (src.includes(STORAGE_HINT)) {
      add(src, img.getAttribute('alt') ?? '')
      img.remove()
    }
  }

  return { html: root.innerHTML, attachments }
}

export function mergeNoticeAttachments(html: string, attachments: NoticeAttachment[]): string {
  const { html: body } = splitNoticeAttachments(html)
  const blocks = attachments
    .filter((item) => item.url.trim())
    .map(
      (item) =>
        `<div class="${ATTACH_CLASS}"><a href="${escapeAttr(item.url)}">${escapeText(item.name || fileNameFromUrl(item.url))}</a></div>`,
    )
    .join('')
  return `${body}${blocks}`
}

function fileNameFromUrl(url: string): string {
  try {
    const path = new URL(url).pathname
    const last = decodeURIComponent(path.split('/').pop() ?? '')
    return last.replace(/^[0-9a-f-]{8,}-/i, '') || '첨부파일'
  } catch {
    return '첨부파일'
  }
}

function escapeAttr(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;')
}

function escapeText(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

export function sanitizeNoticeHtml(html: string): string {
  if (!html.trim()) return ''
  const doc = new DOMParser().parseFromString(`<div id="onda-root">${html}</div>`, 'text/html')
  const root = doc.getElementById('onda-root')
  if (!root) return ''
  cleanNode(root)
  return root.innerHTML
}

function cleanNode(node: Element) {
  const children = Array.from(node.childNodes)
  for (const child of children) {
    if (child.nodeType === Node.ELEMENT_NODE) {
      const el = child as Element
      const tag = el.tagName
      if (!ALLOWED_TAGS.has(tag)) {
        const parent = el.parentNode
        while (el.firstChild) parent?.insertBefore(el.firstChild, el)
        parent?.removeChild(el)
        continue
      }
      for (const attr of Array.from(el.attributes)) {
        const name = attr.name.toLowerCase()
        if (name.startsWith('on') || name === 'style') el.removeAttribute(attr.name)
      }
      if (tag === 'IMG') {
        const src = el.getAttribute('src') ?? ''
        if (!isSafeSrc(src)) {
          el.remove()
          continue
        }
        el.removeAttribute('srcset')
      }
      if (tag === 'A') {
        const href = el.getAttribute('href') ?? ''
        if (!/^https?:\/\//i.test(href)) el.removeAttribute('href')
        el.setAttribute('target', '_blank')
        el.setAttribute('rel', 'noopener noreferrer')
      }
      cleanNode(el)
    }
  }
}

function isSafeSrc(src: string): boolean {
  return /^(https?:\/\/|data:image\/)/i.test(src)
}

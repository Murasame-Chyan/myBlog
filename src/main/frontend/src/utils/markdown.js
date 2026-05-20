import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({ breaks: true, gfm: true })

export function renderMarkdown(md) {
  if (!md) return ''
  return DOMPurify.sanitize(marked.parse(md), {
    ALLOWED_TAGS: ['h1','h2','h3','h4','h5','h6','p','br','hr','ul','ol','li',
      'blockquote','pre','code','strong','em','del','ins','sub','sup',
      'a','img','table','thead','tbody','tr','th','td','div','span'],
    ALLOWED_ATTR: ['href','src','alt','title','class','id','target','rel']
  })
}

export function extractBrief(md, max = 200) {
  if (!md) return ''
  const text = md.replace(/[#*`>\[\]()!_~|]/g, '').replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim()
  return text.length > max ? text.substring(0, max) + '...' : text
}

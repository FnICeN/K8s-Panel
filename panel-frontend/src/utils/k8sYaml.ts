export type ResourceType = 'Pod' | 'Deployment' | 'Service' | 'Namespace'

export interface K8sYamlSummary {
  kind?: ResourceType
  rawKind?: string
  name?: string
  namespace?: string
  documentCount: number
}

const SUPPORTED_KINDS = new Set<string>(['Pod', 'Deployment', 'Service', 'Namespace'])

export function summarizeK8sYaml(yaml: string): K8sYamlSummary {
  const documents = splitDocuments(yaml.replace(/^\uFEFF/, '').replace(/\r\n/g, '\n'))
  const first = documents[0] ?? ''
  const rawKind = readTopLevel(first, 'kind')
  return {
    kind: rawKind && SUPPORTED_KINDS.has(rawKind) ? (rawKind as ResourceType) : undefined,
    rawKind,
    name: readMetadata(first, 'name'),
    namespace: readMetadata(first, 'namespace'),
    documentCount: documents.length,
  }
}

function splitDocuments(yaml: string): string[] {
  const documents: string[] = []
  let current: string[] = []
  for (const line of yaml.split('\n')) {
    if (/^---\s*(#.*)?$/.test(line)) {
      pushDocument(documents, current)
      current = []
      continue
    }
    current.push(line)
  }
  pushDocument(documents, current)
  return documents
}

function pushDocument(documents: string[], lines: string[]) {
  const content = lines.join('\n').trim()
  if (content) {
    documents.push(content)
  }
}

function readTopLevel(yaml: string, field: string): string | undefined {
  for (const line of yaml.split('\n')) {
    if (!line.trim() || /^\s*#/.test(line) || /^\s/.test(line)) {
      continue
    }
    const match = line.match(new RegExp(`^${field}:\\s*(.*)$`))
    if (!match) {
      continue
    }
    return cleanScalar(match[1] ?? '')
  }
  return undefined
}

function readMetadata(yaml: string, field: 'name' | 'namespace'): string | undefined {
  let inMetadata = false
  let childIndent: number | null = null
  for (const line of yaml.split('\n')) {
    if (!line.trim() || /^\s*#/.test(line)) {
      continue
    }
    const indent = line.search(/\S/)
    if (!inMetadata) {
      if (indent === 0 && /^metadata:\s*(#.*)?$/.test(line.trim())) {
        inMetadata = true
      }
      continue
    }
    if (indent === 0) {
      break
    }
    if (childIndent === null) {
      childIndent = indent
    }
    if (indent !== childIndent) {
      continue
    }
    const match = line.trim().match(new RegExp(`^${field}:\\s*(.*)$`))
    if (match) {
      return cleanScalar(match[1] ?? '')
    }
  }
  return undefined
}

function cleanScalar(value: string): string | undefined {
  let result = value.trim()
  let quote: '"' | "'" | null = null
  for (let index = 0; index < result.length; index += 1) {
    const char = result[index]
    if (quote) {
      if (char === quote && result[index - 1] !== '\\') {
        quote = null
      }
      continue
    }
    if (char === '"' || char === "'") {
      quote = char
      continue
    }
    if (char === '#') {
      result = result.slice(0, index).trim()
      break
    }
  }
  if (
    (result.startsWith('"') && result.endsWith('"') && result.length >= 2) ||
    (result.startsWith("'") && result.endsWith("'") && result.length >= 2)
  ) {
    result = result.slice(1, -1)
  }
  return result || undefined
}

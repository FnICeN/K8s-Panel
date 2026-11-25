import http from 'node:http'
import type { IncomingHttpHeaders, IncomingMessage, ServerResponse } from 'node:http'
import type { Plugin, ViteDevServer } from 'vite'

const GRAFANA_HOST = '192.168.5.96'
const GRAFANA_PORT = 30300
const PREFIX = '/grafana-proxy'

function rewriteLocation(location: string): string {
  if (location.startsWith('/grafana-proxy')) return location
  if (location.startsWith('/')) return `${PREFIX}${location}`
  return location
    .replace(`http://${GRAFANA_HOST}:${GRAFANA_PORT}`, PREFIX)
    .replace('http://localhost:3000', PREFIX)
}

function rewriteCookies(setCookie: string | string[]): string[] {
  const list = Array.isArray(setCookie) ? setCookie : [setCookie]
  return list.map((cookie) =>
    cookie
      .replace(/;\s*Domain=[^;]*/gi, '')
      .replace(/;\s*Secure/gi, '')
      .replace(/;\s*Path=\/[^;]*/i, `; Path=${PREFIX}`),
  )
}

function rewriteHtml(html: string, publicOrigin: string): string {
  return html
    .replaceAll('<base href="/" />', `<base href="${PREFIX}/" />`)
    .replaceAll('<base href="/">', `<base href="${PREFIX}/">`)
    .replaceAll('http://localhost:3000', publicOrigin)
    .replaceAll(`http://${GRAFANA_HOST}:${GRAFANA_PORT}`, publicOrigin)
    .replaceAll('"appSubUrl":""', `"appSubUrl":"${PREFIX}"`)
}

function attachGrafanaProxy(server: { middlewares: ViteDevServer['middlewares'] }) {
  server.middlewares.use((req, res, next) => {
    if (!req.url?.startsWith(PREFIX)) {
      next()
      return
    }

    const targetPath = req.url.slice(PREFIX.length) || '/'
    const headers: IncomingHttpHeaders = { ...req.headers }
    headers.host = `${GRAFANA_HOST}:${GRAFANA_PORT}`
    headers['accept-encoding'] = 'identity'
    delete headers.origin

    const proxyReq = http.request(
      {
        hostname: GRAFANA_HOST,
        port: GRAFANA_PORT,
        path: targetPath.startsWith('/') ? targetPath : `/${targetPath}`,
        method: req.method,
        headers,
      },
      (proxyRes) => {
        handleProxyResponse(req, res, proxyRes)
      },
    )

    proxyReq.on('error', (error) => {
      res.statusCode = 502
      res.setHeader('content-type', 'text/plain; charset=utf-8')
      res.end(`云网监控仪表盘代理失败：无法连接 Grafana（${GRAFANA_HOST}:${GRAFANA_PORT}）\n${error.message}`)
    })

    req.pipe(proxyReq)
  })
}

function handleProxyResponse(req: IncomingMessage, res: ServerResponse, proxyRes: IncomingMessage) {
  const headers = { ...proxyRes.headers }
  delete headers['x-frame-options']
  delete headers['content-security-policy']
  delete headers['content-security-policy-report-only']

  if (headers.location) {
    headers.location = rewriteLocation(String(headers.location))
  }
  if (headers['set-cookie']) {
    headers['set-cookie'] = rewriteCookies(headers['set-cookie'])
  }

  const contentType = String(headers['content-type'] || '')
  if (contentType.includes('text/html')) {
    const chunks: Buffer[] = []
    proxyRes.on('data', (chunk) => chunks.push(chunk as Buffer))
    proxyRes.on('end', () => {
      const origin = `http://${req.headers.host || 'localhost:5173'}${PREFIX}`
      const html = rewriteHtml(Buffer.concat(chunks).toString('utf8'), origin)
      const body = Buffer.from(html, 'utf8')
      headers['content-length'] = String(body.length)
      delete headers['transfer-encoding']
      res.writeHead(proxyRes.statusCode || 200, headers)
      res.end(body)
    })
    return
  }

  res.writeHead(proxyRes.statusCode || 200, headers)
  proxyRes.pipe(res)
}

export function grafanaProxyPlugin(): Plugin {
  return {
    name: 'grafana-proxy',
    configureServer(server) {
      attachGrafanaProxy(server)
    },
    configurePreviewServer(server) {
      attachGrafanaProxy(server)
    },
  }
}

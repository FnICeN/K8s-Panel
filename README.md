K8s管理客户端，集成部署在集群内部的Keycloak认证系统进行OIDC认证

# 快速开始

首先克隆项目到本地：

```bash
git clone git@github.com:FnICeN/K8s-Panel.git
```

## 配置集群参数

修改`src/main/resources/application.yml`文件：

```yaml
# Redis配置
data:
  redis:
    host: localhost
    port: 6379
    # 如果Redis未配置密码，则留空
    password:
    ttl: 3600
    
# OIDC配置
oidc:
  issuer: http://localhost:8081/realms/auth-demo
  client-id: k8s-auth
  client-secret: xxx
  scope: openid
  
# 集群配置
cluster:
  cluster-url: http://localhost:6443
  
# ClickHouse 审计数据源
clickhouse:
  url: http://localhost:8123
  database: falco
  username: default
  password: xxx
```

## 启动Redis

本项目启用了基于Redis的分布式Session，因此需要启动本地的Redis进程：

```bash
cd /your/redis/path
redis-server.exe
```

## 启动前后端

```bash
cd K8s-panel
```

启动后端：

```bash
mvn clean package
cd target
java -jar panel-0.0.1-SNAPSHOT.jar
```

启动前端（调试模式）：

```bash
cd panel-frontend
npm install
npm run dev
```

## 审计告警时间范围查询

审计告警页面默认查询近 24 小时，可用时间范围选择器查询 ClickHouse 中仍保留的历史告警。后端先统计所选范围内的全部匹配告警数及红橙蓝级别条数，再按事件时间、UUID 倒序返回当前页，每页 10 条；分页控件显示完整页数并支持直接跳页。实时告警通知仍由 SSE 独立推送。

后端接口 `GET /api/audit/alerts/search` 支持 `start`、`end`、`rule`、`namespace`、`priority`、`limit`、`offset`、`includeSummary` 参数。`start` 和 `end` 使用 ISO-8601 UTC 时间，同时传入；时间范围为 `[start, end)`。未指定时间时默认近 24 小时。首次查询默认统计并返回 `total`、`redCount`、`orangeCount`、`blueCount` 和 `items`；翻页保持相同条件并传 `includeSummary=false`，只查询 `items`，统计字段返回 `null`。

查询使用时间上下界限定范围，分页无需修改 ClickHouse 表结构。可查询的最早时间仍取决于 ClickHouse 的数据保留策略。若要求翻页期间对迟到写入的数据也保持严格的同一快照，需要额外的入库时间或快照机制。

选择时间范围后，可在页面上预览或下载该时间段的审计 PDF 汇总报告。报告包含总数、红橙蓝级别条数、各严重程度和告警规则的条数，以及全部红色告警的详细内容。接口为 `GET /api/audit/reports/period?start=...&end=...`；仅在点击时生成，不写入单条告警报告的历史目录。

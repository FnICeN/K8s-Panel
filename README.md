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

# freelxcserver

## 项目简介

freelxcserver 是一个基于 Spring Boot 的后端管理与服务平台，采用多模块 Maven 结构，面向 LXC 相关业务、系统管理、任务调度、对象存储、短信能力与监控扩展。

## 版本与运行环境

- 当前版本：4.3.1
- JDK：8+
- Maven：3.6+
- Spring Boot：2.7.5
- 默认端口：8088（见 `lxc-admin/src/main/resources/application.yml`）

## 模块结构

- `lxc-admin`：主启动模块（Web 入口）
- `lxc-framework`：框架核心（Web、Undertow、AOP、Actuator 等）
- `lxc-system`：系统业务模块
- `lxc-common`：公共工具与通用能力
- `lxc-job`：任务调度执行模块（XXL-Job）
- `lxc-generator`：代码生成模块（Velocity）
- `lxc-machine`：机器/LXC 相关业务模块
- `lxc-lxd-api`：LXD API 客户端模块
- `lxc-oss`：对象存储模块
- `lxc-sms`：短信模块
- `lxc-demo`：示例功能模块
- `lxc-extend/lxc-monitor-admin`：监控中心扩展服务
- `lxc-extend/lxc-xxl-job-admin`：XXL-Job 管理端扩展服务

## 实际使用的核心组件

以下为仓库 `pom.xml` 与配置文件中已落地的主要组件：

- Web 容器：Undertow
- 权限认证：Sa-Token、Sa-Token-JWT
- 数据访问：MyBatis-Plus、dynamic-datasource、Druid、p6spy
- 缓存与分布式能力：Redis、Redisson、Lock4j
- 任务调度：XXL-Job
- 服务监控：Spring Boot Actuator、Spring Boot Admin
- 文档能力：SpringDoc OpenAPI
- 文件存储：AWS S3 SDK（兼容 MinIO 场景）
- 短信能力：阿里云短信、腾讯云短信（按需启用）
- 工具库：Hutool、Lombok、Jackson
- Excel：EasyExcel、Apache POI
- 链路日志：TLog
- 其他：Kafka Client、Telegram Bots、JSch

## 数据库支持

代码与依赖已包含以下数据库驱动支持：

- MySQL（默认）
- Oracle
- PostgreSQL
- SQL Server

默认开发配置以 MySQL 为主（见 `lxc-admin/src/main/resources/application-dev.yml`）。

## 快速开始

### 1. 准备基础服务

至少准备以下组件：

- MySQL
- Redis

可选组件：

- MinIO（对象存储）
- 监控中心（lxc-monitor-admin）
- XXL-Job Admin（lxc-xxl-job-admin）

仓库已提供 Docker 编排示例：

- `script/docker/docker-compose.yml`
- `script/docker/database.yml`

### 2. 初始化数据库

可使用仓库中的 SQL 文件初始化（按你的实际库类型选择）：

- `lxc_sql/`
- `script/sql/`

### 3. 修改配置

按环境修改以下配置文件中的数据库、Redis、任务调度与监控地址：

- `lxc-admin/src/main/resources/application-dev.yml`
- `lxc-admin/src/main/resources/application-prod.yml`

### 4. 构建与启动

在仓库根目录执行：

```bash
mvn clean package -DskipTests
java -jar lxc-admin/target/lxc-admin.jar
```

启动类：`com.lxc.LxcApplication`

## 生产部署建议

- 使用 `prod` 配置并替换默认账号密码与密钥
- 关闭不必要的调试/测试开关
- 将数据库、Redis、短信、对象存储等敏感配置改为环境变量或外部密钥管理
- 为 `lxc-admin`、`lxc-monitor-admin`、`lxc-xxl-job-admin` 分别规划独立端口与反向代理

## 说明

- 本仓库当前仅包含后端与扩展服务代码，前端不在此仓库内
- 如需精简功能，可按模块在根 `pom.xml` 中调整构建模块与依赖

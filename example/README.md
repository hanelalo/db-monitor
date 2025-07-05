# Database Monitor Example

这是一个使用 Database Monitor Spring Boot Starter 的示例项目。

## 运行步骤

### 1. 启动应用

```bash
cd example
mvn spring-boot:run
```

### 2. 初始化测试数据

```bash
curl -X POST http://localhost:8080/api/test/init
```

### 3. 查看监控数据

等待1-2分钟后，访问以下接口查看监控数据：

#### 获取监控统计数据
```bash
curl http://localhost:8080/api/db-monitor/statistics
```

#### 获取Prometheus格式指标
```bash
curl http://localhost:8080/api/db-monitor/metrics
```

#### 获取JSON格式指标
```bash
curl http://localhost:8080/api/db-monitor/metrics/json
```

#### 获取健康状态
```bash
curl http://localhost:8080/api/db-monitor/health
```

### 4. 模拟数据增长

#### 添加用户数据
```bash
curl -X POST http://localhost:8080/api/test/users?count=10
```

#### 添加订单数据
```bash
curl -X POST http://localhost:8080/api/test/orders?count=20
```

### 5. 手动触发监控任务

```bash
curl -X POST http://localhost:8080/api/db-monitor/trigger
```

## 可用的接口

### 测试接口

- `POST /api/test/init` - 初始化测试数据
- `POST /api/test/users?count=5` - 添加用户数据
- `POST /api/test/orders?count=10` - 添加订单数据
- `GET /api/test/tables` - 获取所有表
- `GET /api/test/users/count` - 获取用户数量
- `GET /api/test/orders/count` - 获取订单数量

### 监控接口

- `GET /api/db-monitor/statistics` - 获取监控统计数据
- `GET /api/db-monitor/statistics/{tableName}` - 获取指定表的监控数据
- `GET /api/db-monitor/metrics` - 获取Prometheus格式指标
- `GET /api/db-monitor/metrics/json` - 获取JSON格式指标
- `GET /api/db-monitor/health` - 获取健康状态
- `POST /api/db-monitor/trigger` - 手动触发监控任务
- `DELETE /api/db-monitor/cleanup` - 清理过期数据

### 系统接口

- `GET /actuator/health` - 系统健康检查
- `GET /actuator/metrics` - 系统指标
- `GET /actuator/prometheus` - Prometheus格式指标

## H2数据库控制台

访问 http://localhost:8080/h2-console

- JDBC URL: `jdbc:h2:mem:testdb`
- User Name: `sa`
- Password: （空）

## 监控配置

当前配置监控以下表：
- `user_*` - 所有以 user_ 开头的表
- `order_info` - 订单表
- `product_*` - 所有以 product_ 开头的表

监控间隔为1分钟（用于演示）。

## 注意事项

1. 应用启动后需要等待1-2分钟才能看到监控数据
2. 监控数据存储在 `db_monitor_statistics` 表中
3. H2数据库是内存数据库，重启应用后数据会丢失
4. 可以通过修改 `application.yml` 中的配置来调整监控参数
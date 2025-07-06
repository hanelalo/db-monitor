# 轻量级架构设计指南

## 概述

本项目采用轻量级架构设计，避免引入重量级的ORM框架（如MyBatis、JPA、Hibernate），以减少对使用方项目的依赖影响。

## 设计原则

### 1. 最小依赖原则
- **只依赖Spring Boot核心组件**：主要使用 `spring-boot-starter-jdbc`
- **避免重量级框架**：不使用MyBatis、JPA、Hibernate等ORM框架
- **可选依赖**：将非核心功能设为可选依赖

### 2. 轻量级数据访问层
使用 `JdbcTemplate` 作为数据访问的核心，具有以下优势：

**优势：**
- ✅ 轻量级，性能优秀
- ✅ SQL控制精确，便于优化
- ✅ 学习成本低
- ✅ 与Spring Boot完美集成
- ✅ 不引入额外的映射配置

**实现方式：**
- `JdbcMonitorConfigRepository` - 监控配置数据访问
- `JdbcTableOperationRepository` - 表操作数据访问  
- `JdbcDbMonitorStatisticsRepository` - 统计数据访问

## 依赖管理策略

### 1. 核心依赖（必需）
```xml
<!-- Spring Boot 核心依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- JDBC 支持 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<!-- 配置处理 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

### 2. 可选依赖（按需引入）
```xml
<!-- XXL-Job 支持（可选） -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>${xxl.job.version}</version>
    <optional>true</optional>
</dependency>

<!-- Prometheus 指标（可选） -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <version>${micrometer.version}</version>
    <optional>true</optional>
</dependency>

<!-- 数据库驱动（可选，用户自行选择） -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### 3. 开发工具依赖
```xml
<!-- Lombok（编译时） -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Jackson（JSON处理） -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

## 架构优势

### 1. 对使用方的影响最小
- **无额外ORM依赖**：不会强制引入MyBatis、JPA等框架
- **版本冲突风险低**：依赖的都是Spring Boot核心组件
- **启动速度快**：没有复杂的映射配置和代理生成

### 2. 性能优秀
- **直接SQL执行**：没有ORM的额外开销
- **连接池复用**：使用Spring Boot的数据源管理
- **内存占用小**：没有复杂的缓存和映射对象

### 3. 易于维护和扩展
- **代码简洁**：直接的SQL操作，易于理解
- **调试友好**：SQL执行过程透明
- **扩展灵活**：可以轻松添加新的数据访问方法

## 使用建议

### 1. 对于组件开发者
```java
// 推荐：使用轻量级Repository
@Autowired
private JdbcMonitorConfigRepository configRepository;

// 避免：引入重量级ORM
// @Autowired
// private MonitorConfigMapper configMapper; // MyBatis
```

### 2. 对于使用方
```yaml
# 最小配置
db:
  monitor:
    enabled: true
    data-source-name: primary
    table-names: 
      - user_table
      - order_table

# 可选功能配置
db:
  monitor:
    xxl-job:
      enabled: false  # 如果不需要XXL-Job
    metrics:
      enabled: false  # 如果不需要Prometheus指标
```

### 3. 数据库支持
组件支持多种数据库，用户只需要添加对应的驱动：

```xml
<!-- MySQL -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>

<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- H2 (测试用) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
</dependency>
```

## 迁移指南

### 从MyBatis迁移到JdbcTemplate

**之前（MyBatis）：**
```java
@Autowired
private MonitorConfigMapper configMapper;

List<MonitorConfig> configs = configMapper.findAll();
```

**现在（JdbcTemplate）：**
```java
@Autowired
private JdbcMonitorConfigRepository configRepository;

List<MonitorConfig> configs = configRepository.findAll();
```

### 配置变更
```yaml
# 移除MyBatis配置
# mybatis:
#   mapper-locations: classpath:mapper/*.xml

# 保持Spring Boot JDBC配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test
    username: root
    password: password
```

## 性能对比

| 特性 | JdbcTemplate | MyBatis | JPA/Hibernate |
|------|-------------|---------|---------------|
| 启动时间 | 快 | 中等 | 慢 |
| 内存占用 | 小 | 中等 | 大 |
| SQL控制 | 完全控制 | 完全控制 | 有限控制 |
| 学习成本 | 低 | 中等 | 高 |
| 依赖大小 | 小 | 中等 | 大 |
| 版本兼容性 | 好 | 中等 | 复杂 |

## 总结

通过采用轻量级架构设计，本组件能够：

1. **最小化依赖影响**：不会给使用方项目带来额外的依赖负担
2. **保持高性能**：直接的SQL操作，没有ORM的额外开销
3. **易于集成**：与Spring Boot项目无缝集成
4. **便于维护**：代码简洁，逻辑清晰
5. **灵活扩展**：可以根据需要添加新功能

这种设计特别适合作为Spring Boot Starter组件，为其他项目提供数据库监控功能而不引入过多的复杂性。

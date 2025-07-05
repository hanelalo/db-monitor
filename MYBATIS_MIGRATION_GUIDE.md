# MyBatis 框架迁移指南

## 概述

本项目已成功从 JPA/Hibernate 迁移到 MyBatis 框架。本文档详细说明了迁移过程中的所有更改和配置。

## 主要更改

### 1. 依赖更改 (pom.xml)

**移除的依赖:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

**新增的依赖:**
```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.2.2</version>
</dependency>
```

### 2. 实体类更改 (DbMonitorStatistics.java)

**移除的 JPA 注解:**
- `@Entity`
- `@Table`
- `@Id`
- `@GeneratedValue`
- `@Column`
- `@PrePersist`

**现在的实体类:**
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbMonitorStatistics {
    private Long id;
    private String dataSourceName;
    private String tableName;
    // ... 其他字段
}
```

### 3. Repository 接口更改

**之前 (JPA):**
```java
@Repository
public interface DbMonitorStatisticsRepository extends JpaRepository<DbMonitorStatistics, Long> {
    // JPA查询方法
}
```

**现在 (MyBatis):**
```java
@Mapper
public interface DbMonitorStatisticsRepository {
    @Insert("INSERT INTO db_monitor_statistics (...) VALUES (...)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DbMonitorStatistics statistics);
    
    @Select("SELECT * FROM db_monitor_statistics WHERE id = #{id}")
    DbMonitorStatistics findById(Long id);
    
    // 其他MyBatis注解方法
}
```

### 4. 配置更改

**application.yml 配置:**

**移除的 JPA 配置:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
```

**新增的 MyBatis 配置:**
```yaml
spring:
  mybatis:
    mapper-locations: classpath:mapper/*.xml
    type-aliases-package: com.github.starter.dbmonitor.entity
    configuration:
      map-underscore-to-camel-case: true
      cache-enabled: true
      lazy-loading-enabled: true
      # 其他MyBatis配置...
```

### 5. Mapper XML 文件

新增了 `src/main/resources/mapper/DbMonitorStatisticsMapper.xml` 文件，包含：
- 基础结果映射 (ResultMap)
- 基础列定义 (SQL片段)
- 完整的 CRUD 操作
- 复杂查询语句
- 动态 SQL 支持
- 批量操作支持

### 6. 自动配置更改

在 `DbMonitorAutoConfiguration.java` 中添加了：
```java
@MapperScan("com.github.starter.dbmonitor.repository")
```

### 7. Service 层更改

**之前 (JPA):**
```java
statisticsRepository.save(statistics);
```

**现在 (MyBatis):**
```java
statistics.setCreatedTime(LocalDateTime.now());
statisticsRepository.insert(statistics);
```

## MyBatis 特性和优势

### 1. 注解方式 vs XML 方式

**注解方式 (用于简单查询):**
```java
@Select("SELECT * FROM db_monitor_statistics WHERE id = #{id}")
DbMonitorStatistics findById(Long id);
```

**XML 方式 (用于复杂查询):**
```xml
<select id="findByCondition" resultMap="BaseResultMap">
    SELECT * FROM db_monitor_statistics
    <where>
        <if test="dataSourceName != null">
            AND data_source_name = #{dataSourceName}
        </if>
        <if test="tableName != null">
            AND table_name = #{tableName}
        </if>
    </where>
</select>
```

### 2. 动态 SQL 支持

MyBatis 提供了强大的动态 SQL 功能：
- `<if>` 条件判断
- `<choose>`, `<when>`, `<otherwise>` 选择语句
- `<foreach>` 循环
- `<where>` 智能去除多余的 AND/OR

### 3. 缓存机制

- **一级缓存**: 默认启用，SqlSession 级别
- **二级缓存**: 可配置启用，Mapper 级别

### 4. 批量操作

支持高效的批量插入和更新操作：
```java
int batchInsert(@Param("list") List<DbMonitorStatistics> statisticsList);
```

## 使用方法

### 1. 基本 CRUD 操作

```java
// 插入记录
DbMonitorStatistics statistics = new DbMonitorStatistics();
statistics.setDataSourceName("primary");
statistics.setTableName("user_info");
statistics.setCreatedTime(LocalDateTime.now());
// 设置其他字段...
statisticsRepository.insert(statistics);

// 查询记录
DbMonitorStatistics result = statisticsRepository.findById(1L);

// 更新记录
statistics.setTableName("user_profile");
statisticsRepository.update(statistics);

// 删除记录
statisticsRepository.deleteById(1L);
```

### 2. 复杂查询

```java
// 动态条件查询
List<DbMonitorStatistics> results = statisticsRepository.findByCondition(
    "primary", "user_*", startTime, endTime, "MINUTES"
);

// 批量插入
List<DbMonitorStatistics> statisticsList = Arrays.asList(statistics1, statistics2);
statisticsRepository.batchInsert(statisticsList);
```

### 3. 自定义查询

你可以在 XML 文件中添加自定义查询：
```xml
<select id="findTopTablesByIncrement" resultMap="BaseResultMap">
    SELECT * FROM db_monitor_statistics
    WHERE data_source_name = #{dataSourceName}
    ORDER BY increment_count DESC
    LIMIT #{limit}
</select>
```

然后在 Repository 接口中声明方法：
```java
List<DbMonitorStatistics> findTopTablesByIncrement(
    @Param("dataSourceName") String dataSourceName,
    @Param("limit") int limit
);
```

## 性能优化建议

### 1. 合理使用缓存
- 启用二级缓存用于频繁查询的数据
- 配置合适的缓存过期策略

### 2. 优化 SQL 语句
- 使用索引优化查询性能
- 避免 N+1 查询问题
- 使用批量操作减少数据库交互

### 3. 连接池配置
- 配置合适的连接池大小
- 设置合理的超时时间

### 4. 日志配置
- 在开发环境启用 SQL 日志
- 生产环境关闭详细日志以提高性能

## 注意事项

### 1. 字段映射
- MyBatis 支持驼峰命名自动映射
- 可以通过 `@Results` 或 XML `<resultMap>` 自定义映射关系

### 2. 事务管理
- 继续使用 Spring 的 `@Transactional` 注解
- MyBatis 与 Spring 事务管理完全兼容

### 3. 类型处理
- MyBatis 自动处理大多数 Java 类型
- 可以自定义 TypeHandler 处理特殊类型

### 4. 分页处理
- 使用 MyBatis 分页插件如 PageHelper
- 或者手动编写分页 SQL

## 故障排除

### 1. 常见问题
- **Mapper 接口未被扫描**: 确保 `@MapperScan` 注解配置正确
- **SQL 语法错误**: 检查 XML 文件中的 SQL 语句
- **参数映射错误**: 确保 `@Param` 注解使用正确

### 2. 调试技巧
- 启用 SQL 日志输出
- 使用 MyBatis Generator 生成基础 CRUD 代码
- 利用 IDE 的 MyBatis 插件进行语法检查

## 总结

MyBatis 迁移已经完成，主要优势包括：
- 更好的 SQL 控制能力
- 优秀的动态 SQL 支持
- 高性能的批量操作
- 灵活的缓存机制
- 简洁的注解和 XML 配置

项目现在完全基于 MyBatis 运行，所有原有功能都已经正确迁移并保持兼容。
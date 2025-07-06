# 数据库监控组件测试指南

## 📋 测试概述

本项目包含完整的测试套件，涵盖单元测试、集成测试和端到端测试，确保数据库监控组件的可靠性和稳定性。

## 🧪 测试结构

```
src/test/java/
├── com/github/starter/dbmonitor/
│   ├── controller/           # 控制器层测试
│   │   └── MonitorConfigControllerTest.java
│   ├── service/             # 服务层测试
│   │   ├── DatabaseSecurityServiceTest.java
│   │   ├── MonitorConfigServiceTest.java
│   │   └── DbMonitorServiceTest.java
│   ├── repository/          # 数据访问层测试
│   │   └── MonitorConfigRepositoryTest.java
│   ├── integration/         # 集成测试
│   │   └── DbMonitorIntegrationTest.java
│   └── DbMonitorTestSuite.java  # 测试套件
└── resources/
    ├── application-test.yml  # 测试配置
    ├── schema.sql           # 测试数据库结构
    └── data.sql            # 测试数据
```

## 🚀 运行测试

### 方式一：使用脚本运行（推荐）

```bash
./run-tests.sh
```

### 方式二：使用Maven命令

```bash
# 运行所有测试
mvn clean test verify

# 只运行单元测试
mvn test

# 只运行集成测试
mvn verify

# 生成代码覆盖率报告
mvn jacoco:report
```

### 方式三：运行特定测试

```bash
# 运行特定测试类
mvn test -Dtest=DatabaseSecurityServiceTest

# 运行特定测试方法
mvn test -Dtest=DatabaseSecurityServiceTest#testValidTableName

# 运行测试套件
mvn test -Dtest=DbMonitorTestSuite
```

## 📊 测试覆盖范围

### 1. 单元测试

#### DatabaseSecurityServiceTest
- ✅ 表名验证（有效/无效）
- ✅ 列名验证（有效/无效）
- ✅ 数据源名称验证
- ✅ SQL关键字检测
- ✅ 长度限制检查
- ✅ 特殊字符处理
- ✅ 大小写不敏感检测

#### MonitorConfigServiceTest
- ✅ 配置创建（成功/失败场景）
- ✅ 配置更新
- ✅ 配置查询（按ID、按名称）
- ✅ 配置删除
- ✅ 启用/禁用配置
- ✅ 批量操作
- ✅ 配置验证
- ✅ 表列信息获取
- ✅ 时间字段检测

#### DbMonitorServiceTest
- ✅ 监控任务执行
- ✅ 统计数据生成
- ✅ 时间范围计算
- ✅ 异常处理
- ✅ 事务管理
- ✅ 数据清理

#### MonitorConfigControllerTest
- ✅ REST API端点测试
- ✅ 请求参数验证
- ✅ 响应格式验证
- ✅ 错误处理
- ✅ 安全验证集成

#### MonitorConfigRepositoryTest
- ✅ 数据库操作（CRUD）
- ✅ 查询方法
- ✅ 异常处理
- ✅ 事务回滚

### 2. 集成测试

#### DbMonitorIntegrationTest
- ✅ 端到端配置管理
- ✅ 数据库连接测试
- ✅ 监控任务执行
- ✅ 统计数据生成
- ✅ 数据持久化
- ✅ 配置验证流程

## 🔧 测试配置

### 测试数据库
- 使用H2内存数据库进行测试
- 自动创建测试表结构
- 预置测试数据

### 测试环境配置
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  profiles:
    active: test
```

## 📈 代码覆盖率

测试套件提供全面的代码覆盖率：

- **行覆盖率**: > 85%
- **分支覆盖率**: > 80%
- **方法覆盖率**: > 90%

查看详细报告：`target/site/jacoco/index.html`

## 🐛 测试数据

### 测试表结构
- `test_table`: 基础测试表
- `user_table`: 用户数据表
- `monitor_config`: 监控配置表
- `db_monitor_statistics`: 统计数据表

### 预置数据
- 5条测试记录
- 3个监控配置（2个启用，1个禁用）
- 4条历史统计数据

## 🔍 测试最佳实践

### 1. 测试命名规范
```java
@Test
void testMethodName_Scenario_ExpectedResult() {
    // 测试实现
}
```

### 2. 测试结构（AAA模式）
```java
@Test
void testExample() {
    // Arrange - 准备测试数据
    
    // Act - 执行被测试的方法
    
    // Assert - 验证结果
}
```

### 3. Mock使用
- 使用`@Mock`注解创建模拟对象
- 使用`when().thenReturn()`设置期望行为
- 使用`verify()`验证方法调用

### 4. 异常测试
```java
@Test
void testException() {
    assertThrows(IllegalArgumentException.class, () -> {
        // 执行会抛出异常的代码
    });
}
```

## 🚨 常见问题

### 1. 测试失败排查
1. 检查测试数据是否正确
2. 验证Mock对象配置
3. 查看日志输出
4. 检查数据库连接

### 2. 性能测试
```java
@Test
@Timeout(value = 5, unit = TimeUnit.SECONDS)
void testPerformance() {
    // 性能敏感的测试
}
```

### 3. 并发测试
```java
@Test
void testConcurrency() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    // 并发测试逻辑
}
```

## 📝 测试报告

### 生成报告
```bash
mvn clean test jacoco:report
```

### 报告位置
- JUnit报告: `target/surefire-reports/`
- 覆盖率报告: `target/site/jacoco/`
- 集成测试报告: `target/failsafe-reports/`

## 🔄 持续集成

测试套件支持CI/CD集成：

```yaml
# GitHub Actions 示例
- name: Run Tests
  run: |
    mvn clean test verify
    mvn jacoco:report
```

## 📚 扩展测试

### 添加新测试
1. 在相应包下创建测试类
2. 继承或使用相应的测试基类
3. 添加到测试套件中
4. 更新文档

### 性能基准测试
```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class PerformanceBenchmark {
    // 性能基准测试
}
```

---

## 🎯 测试目标

通过完整的测试套件，我们确保：

1. **功能正确性**: 所有功能按预期工作
2. **安全性**: 防止SQL注入等安全问题
3. **稳定性**: 异常情况下的正确处理
4. **性能**: 满足性能要求
5. **可维护性**: 代码质量和可读性

运行测试套件是开发流程的重要组成部分，确保每次代码变更都不会破坏现有功能。

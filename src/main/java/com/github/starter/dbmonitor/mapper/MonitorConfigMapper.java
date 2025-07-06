package com.github.starter.dbmonitor.mapper;

import com.github.starter.dbmonitor.entity.MonitorConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 监控配置MyBatis映射器
 */
@Mapper
public interface MonitorConfigMapper {
    
    /**
     * 创建监控配置表
     */
    void createTableIfNotExists();
    
    /**
     * 插入监控配置
     */
    int insert(MonitorConfig config);
    
    /**
     * 更新监控配置
     */
    int update(MonitorConfig config);
    
    /**
     * 根据ID查找监控配置
     */
    MonitorConfig findById(@Param("id") Long id);
    
    /**
     * 根据配置名称查找监控配置
     */
    MonitorConfig findByConfigName(@Param("configName") String configName);
    
    /**
     * 查找所有监控配置
     */
    List<MonitorConfig> findAll();
    
    /**
     * 查找启用的监控配置
     */
    List<MonitorConfig> findAllEnabled();
    
    /**
     * 根据数据源查找监控配置
     */
    List<MonitorConfig> findByDataSourceName(@Param("dataSourceName") String dataSourceName);
    
    /**
     * 根据数据源和表名查找监控配置
     */
    MonitorConfig findByDataSourceNameAndTableName(@Param("dataSourceName") String dataSourceName, 
                                                  @Param("tableName") String tableName);
    
    /**
     * 根据ID删除监控配置
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 启用或禁用监控配置
     */
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled, 
                     @Param("updatedBy") String updatedBy, @Param("updatedTime") LocalDateTime updatedTime);
    
    /**
     * 批量启用或禁用监控配置
     */
    int batchUpdateEnabled(@Param("ids") List<Long> ids, @Param("enabled") Boolean enabled, 
                          @Param("updatedBy") String updatedBy, @Param("updatedTime") LocalDateTime updatedTime);
    
    /**
     * 获取表的所有列信息
     */
    List<String> getTableColumns(@Param("tableName") String tableName);
    
    /**
     * 自动检测表的时间字段
     */
    List<String> detectTimeColumns(@Param("tableName") String tableName);
    
    /**
     * 检查表是否存在
     */
    Integer checkTableExists(@Param("tableName") String tableName);

    /**
     * 检查列是否存在
     */
    Integer checkColumnExists(@Param("tableName") String tableName, @Param("columnName") String columnName);
}
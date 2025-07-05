package com.github.starter.dbmonitor.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 表操作MyBatis映射器
 */
@Mapper
public interface TableOperationMapper {
    
    /**
     * 获取数据库中所有表名
     */
    List<String> getAllTableNames();
    
    /**
     * 使用SHOW TABLES获取表名
     */
    List<String> getTableNamesByShow();
    
    /**
     * 检查表是否存在
     */
    Integer checkTableExists(@Param("tableName") String tableName);
    
    /**
     * 检查表是否存在(通过查询)
     */
    Integer checkTableExistsByQuery(@Param("tableName") String tableName);
    
    /**
     * 获取表的列信息
     */
    List<String> getTableColumns(@Param("tableName") String tableName);
    
    /**
     * 查询表的增量数据
     */
    Long queryTableIncrement(@Param("tableName") String tableName, 
                           @Param("timeColumn") String timeColumn,
                           @Param("startTime") LocalDateTime startTime, 
                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 从INFORMATION_SCHEMA获取平均行大小
     */
    Long getAvgRowSizeFromInformationSchema(@Param("tableName") String tableName);
    
    /**
     * 从SHOW TABLE STATUS获取表状态信息
     */
    Map<String, Object> getTableStatusInfo(@Param("tableName") String tableName);
    
    /**
     * 获取表的结构信息用于估算行大小
     */
    List<Map<String, Object>> getTableSchemaInfo(@Param("tableName") String tableName);
}
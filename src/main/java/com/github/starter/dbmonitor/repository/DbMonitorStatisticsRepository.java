package com.github.starter.dbmonitor.repository;

import com.github.starter.dbmonitor.entity.DbMonitorStatistics;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据库监控统计数据Mapper
 */
@Mapper
public interface DbMonitorStatisticsRepository {
    
    /**
     * 插入新的统计记录
     */
    @Insert("INSERT INTO db_monitor_statistics (data_source_name, table_name, statistic_time, " +
            "start_time, end_time, increment_count, estimated_disk_size_bytes, avg_row_size_bytes, " +
            "interval_type, interval_value, created_time, additional_info) " +
            "VALUES (#{dataSourceName}, #{tableName}, #{statisticTime}, #{startTime}, #{endTime}, " +
            "#{incrementCount}, #{estimatedDiskSizeBytes}, #{avgRowSizeBytes}, #{intervalType}, " +
            "#{intervalValue}, #{createdTime}, #{additionalInfo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DbMonitorStatistics statistics);
    
    /**
     * 根据数据源名称和表名查询最新的统计记录
     */
    @Select("SELECT * FROM db_monitor_statistics WHERE data_source_name = #{dataSourceName} " +
           "AND table_name = #{tableName} ORDER BY statistic_time DESC")
    List<DbMonitorStatistics> findByDataSourceNameAndTableNameOrderByStatisticTimeDesc(
            @Param("dataSourceName") String dataSourceName, 
            @Param("tableName") String tableName);
    
    /**
     * 根据数据源名称查询统计记录
     */
    @Select("SELECT * FROM db_monitor_statistics WHERE data_source_name = #{dataSourceName} " +
           "ORDER BY statistic_time DESC")
    List<DbMonitorStatistics> findByDataSourceNameOrderByStatisticTimeDesc(String dataSourceName);
    
    /**
     * 根据表名查询统计记录
     */
    @Select("SELECT * FROM db_monitor_statistics WHERE table_name = #{tableName} " +
           "ORDER BY statistic_time DESC")
    List<DbMonitorStatistics> findByTableNameOrderByStatisticTimeDesc(String tableName);
    
    /**
     * 根据时间范围查询统计记录
     */
    @Select("SELECT * FROM db_monitor_statistics WHERE statistic_time >= #{startTime} " +
           "AND statistic_time <= #{endTime} ORDER BY statistic_time DESC")
    List<DbMonitorStatistics> findByStatisticTimeBetween(
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据数据源名称、表名和时间范围查询统计记录
     */
    @Select("SELECT * FROM db_monitor_statistics WHERE data_source_name = #{dataSourceName} " +
           "AND table_name = #{tableName} AND statistic_time >= #{startTime} " +
           "AND statistic_time <= #{endTime} ORDER BY statistic_time DESC")
    List<DbMonitorStatistics> findByDataSourceNameAndTableNameAndStatisticTimeBetween(
            @Param("dataSourceName") String dataSourceName,
            @Param("tableName") String tableName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 删除指定时间之前的记录
     */
    @Delete("DELETE FROM db_monitor_statistics WHERE created_time < #{cutoffTime}")
    void deleteByCreatedTimeBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 统计指定数据源和表的最新增量数据
     */
    @Select("SELECT * FROM db_monitor_statistics WHERE data_source_name = #{dataSourceName} " +
           "AND table_name = #{tableName} ORDER BY statistic_time DESC LIMIT 1")
    DbMonitorStatistics findLatestByDataSourceNameAndTableName(
            @Param("dataSourceName") String dataSourceName, 
            @Param("tableName") String tableName);
    
    /**
     * 统计所有表的最新增量数据
     */
    @Select("SELECT table_name, increment_count, statistic_time FROM db_monitor_statistics " +
           "WHERE data_source_name = #{dataSourceName} " +
           "AND statistic_time = (SELECT MAX(statistic_time) FROM db_monitor_statistics s2 " +
           "WHERE s2.data_source_name = #{dataSourceName} AND s2.table_name = db_monitor_statistics.table_name)")
    List<Object[]> findLatestStatisticsByDataSourceName(@Param("dataSourceName") String dataSourceName);
    
    /**
     * 根据ID查询统计记录
     */
    @Select("SELECT * FROM db_monitor_statistics WHERE id = #{id}")
    DbMonitorStatistics findById(Long id);
    
    /**
     * 更新统计记录
     */
    @Update("UPDATE db_monitor_statistics SET data_source_name = #{dataSourceName}, " +
           "table_name = #{tableName}, statistic_time = #{statisticTime}, " +
           "start_time = #{startTime}, end_time = #{endTime}, increment_count = #{incrementCount}, " +
           "estimated_disk_size_bytes = #{estimatedDiskSizeBytes}, avg_row_size_bytes = #{avgRowSizeBytes}, " +
           "interval_type = #{intervalType}, interval_value = #{intervalValue}, " +
           "additional_info = #{additionalInfo} WHERE id = #{id}")
    int update(DbMonitorStatistics statistics);
    
    /**
     * 根据ID删除统计记录
     */
    @Delete("DELETE FROM db_monitor_statistics WHERE id = #{id}")
    int deleteById(Long id);
    
    /**
     * 动态条件查询统计记录
     */
    List<DbMonitorStatistics> findByCondition(@Param("dataSourceName") String dataSourceName,
                                             @Param("tableName") String tableName,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime,
                                             @Param("intervalType") String intervalType);
    
    /**
     * 批量插入统计记录
     */
    int batchInsert(@Param("list") List<DbMonitorStatistics> statisticsList);
}
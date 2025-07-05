package com.github.starter.dbmonitor.repository;

import com.github.starter.dbmonitor.entity.DbMonitorStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据库监控统计数据仓库
 */
@Repository
public interface DbMonitorStatisticsRepository extends JpaRepository<DbMonitorStatistics, Long> {
    
    /**
     * 根据数据源名称和表名查询最新的统计记录
     */
    @Query("SELECT s FROM DbMonitorStatistics s WHERE s.dataSourceName = :dataSourceName " +
           "AND s.tableName = :tableName ORDER BY s.statisticTime DESC")
    List<DbMonitorStatistics> findByDataSourceNameAndTableNameOrderByStatisticTimeDesc(
            @Param("dataSourceName") String dataSourceName, 
            @Param("tableName") String tableName);
    
    /**
     * 根据数据源名称查询统计记录
     */
    List<DbMonitorStatistics> findByDataSourceNameOrderByStatisticTimeDesc(String dataSourceName);
    
    /**
     * 根据表名查询统计记录
     */
    List<DbMonitorStatistics> findByTableNameOrderByStatisticTimeDesc(String tableName);
    
    /**
     * 根据时间范围查询统计记录
     */
    @Query("SELECT s FROM DbMonitorStatistics s WHERE s.statisticTime >= :startTime " +
           "AND s.statisticTime <= :endTime ORDER BY s.statisticTime DESC")
    List<DbMonitorStatistics> findByStatisticTimeBetween(
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据数据源名称、表名和时间范围查询统计记录
     */
    @Query("SELECT s FROM DbMonitorStatistics s WHERE s.dataSourceName = :dataSourceName " +
           "AND s.tableName = :tableName AND s.statisticTime >= :startTime " +
           "AND s.statisticTime <= :endTime ORDER BY s.statisticTime DESC")
    List<DbMonitorStatistics> findByDataSourceNameAndTableNameAndStatisticTimeBetween(
            @Param("dataSourceName") String dataSourceName,
            @Param("tableName") String tableName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 删除指定时间之前的记录
     */
    @Query("DELETE FROM DbMonitorStatistics s WHERE s.createdTime < :cutoffTime")
    void deleteByCreatedTimeBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 统计指定数据源和表的最新增量数据
     */
    @Query("SELECT s FROM DbMonitorStatistics s WHERE s.dataSourceName = :dataSourceName " +
           "AND s.tableName = :tableName ORDER BY s.statisticTime DESC LIMIT 1")
    DbMonitorStatistics findLatestByDataSourceNameAndTableName(
            @Param("dataSourceName") String dataSourceName, 
            @Param("tableName") String tableName);
    
    /**
     * 统计所有表的最新增量数据
     */
    @Query("SELECT s.tableName, s.incrementCount, s.statisticTime FROM DbMonitorStatistics s " +
           "WHERE s.dataSourceName = :dataSourceName " +
           "AND s.statisticTime = (SELECT MAX(s2.statisticTime) FROM DbMonitorStatistics s2 " +
           "WHERE s2.dataSourceName = s.dataSourceName AND s2.tableName = s.tableName)")
    List<Object[]> findLatestStatisticsByDataSourceName(@Param("dataSourceName") String dataSourceName);
}
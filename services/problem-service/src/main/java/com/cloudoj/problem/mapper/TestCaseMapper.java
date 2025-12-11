package com.cloudoj.problem.mapper;

import com.cloudoj.model.entity.problem.TestCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 测试用例Mapper接口
 */
@Mapper
public interface TestCaseMapper {
    
    /**
     * 根据题目ID查询测试用例
     * @param problemId 题目ID
     * @return 测试用例列表
     */
    List<TestCase> selectByProblemId(@Param("problemId") Long problemId);
    
    /**
     * 根据题目ID查询样例测试用例（is_sample=1）
     * @param problemId 题目ID
     * @return 样例测试用例列表
     */
    List<TestCase> selectSamplesByProblemId(@Param("problemId") Long problemId);
    
    /**
     * 根据ID查询测试用例
     * @param id 测试用例ID
     * @return 测试用例
     */
    TestCase selectById(@Param("id") Long id);
    
    /**
     * 插入测试用例
     * @param testCase 测试用例
     * @return 影响行数
     */
    int insert(TestCase testCase);
    
    /**
     * 批量插入测试用例
     * @param list 测试用例列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<TestCase> list);
    
    /**
     * 更新测试用例
     * @param testCase 测试用例
     * @return 影响行数
     */
    int update(TestCase testCase);
    
    /**
     * 删除题目的所有测试用例（逻辑删除）
     * @param problemId 题目ID
     * @return 影响行数
     */
    int deleteByProblemId(@Param("problemId") Long problemId);
    
    /**
     * 根据ID删除测试用例（逻辑删除）
     * @param id 测试用例ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 统计题目的测试用例数量
     * @param problemId 题目ID
     * @return 测试用例数量
     */
    int countByProblemId(@Param("problemId") Long problemId);
}

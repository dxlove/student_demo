package com.demo.studentdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.studentdemo.entity.Student;
import com.demo.studentdemo.vo.StudentGradeVO;
import com.demo.studentdemo.vo.StudentQueryReq;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 学生基本信息 Mapper 接口
 * </p>
 *
 * @author admin
 * @since 2025-07-30
 */
public interface StudentMapper extends BaseMapper<Student> {
    Page<StudentGradeVO> queryAllGrade(@Param("page") Page<StudentGradeVO> page, @Param("req") StudentQueryReq studentQueryReq);

    //public List<StudentGradeVO> queryAllGrade(@Param("req") StudentQueryReq studentQueryReq);
}

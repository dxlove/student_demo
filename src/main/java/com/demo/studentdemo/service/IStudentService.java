package com.demo.studentdemo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.studentdemo.entity.Student;
import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.studentdemo.vo.PageQueryParam;
import com.demo.studentdemo.vo.StudentGradeVO;
import com.demo.studentdemo.vo.StudentQueryReq;

import java.util.List;

/**
 * <p>
 * 学生基本信息 服务类
 * </p>
 *
 * @author admin
 * @since 2025-07-30
 */
public interface IStudentService extends IService<Student> {
    IPage<StudentGradeVO> queryAllGrade(PageQueryParam<StudentQueryReq> studentQueryReq);
}

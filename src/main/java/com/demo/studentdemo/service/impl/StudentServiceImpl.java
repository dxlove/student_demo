package com.demo.studentdemo.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.studentdemo.entity.Student;
import com.demo.studentdemo.mapper.StudentMapper;
import com.demo.studentdemo.service.IStudentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.studentdemo.vo.PageQueryParam;
import com.demo.studentdemo.vo.StudentGradeVO;
import com.demo.studentdemo.vo.StudentQueryReq;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 学生基本信息 服务实现类
 * </p>
 *
 * @author admin
 * @since 2025-07-30
 */
@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements IStudentService {


    @Override
    public IPage<StudentGradeVO> queryAllGrade(PageQueryParam<StudentQueryReq> studentQueryReq) {
        Page<StudentGradeVO> page = new Page<>(studentQueryReq.getPageNum(), studentQueryReq.getPageSize());
        Page<StudentGradeVO> students = this.baseMapper.queryAllGrade(page, studentQueryReq.getCondition());
        return students;
    }
}

package com.demo.studentdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.studentdemo.entity.Student;
import com.demo.studentdemo.service.IStudentService;
import com.demo.studentdemo.vo.PageQueryParam;
import com.demo.studentdemo.vo.StudentGradeVO;
import com.demo.studentdemo.vo.StudentQueryReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 学生基本信息 前端控制器
 * </p>
 *
 * @author admin
 * @since 2025-07-30
 */
@RestController
@RequestMapping("/student")
@Slf4j
public class StudentController {

    @Autowired
    private IStudentService studentService;

    /**
     * 获取学生列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<Student>> list() {
        List<Student> students = studentService.list();
        return ResponseEntity.ok(students);
    }

    /**
     * 分页查询学生
     */
    @GetMapping("/page")
    public ResponseEntity<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        
        Page<Student> page = new Page<>(current, size);
        QueryWrapper<Student> queryWrapper = new QueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        
        Page<Student> resultPage = studentService.page(page, queryWrapper);
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", resultPage.getRecords());
        result.put("total", resultPage.getTotal());
        result.put("current", resultPage.getCurrent());
        result.put("size", resultPage.getSize());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 根据ID获取学生信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<Student> getById(@PathVariable Long id) {
        Student student = studentService.getById(id);
        if (student != null) {
            return ResponseEntity.ok(student);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 添加学生
     */
    @PostMapping
    public ResponseEntity<Student> add(@RequestBody Student student) {
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        student.setCreatedAt(now);
        student.setUpdatedAt(now);
        student.setCreatedBy("admin"); // 实际应用中应该从登录用户获取
        student.setUpdatedBy("admin");
        
        boolean success = studentService.save(student);
        if (success) {
            return ResponseEntity.ok(student);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新学生信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<Student> update(@PathVariable Long id, @RequestBody Student student) {
        Student existingStudent = studentService.getById(id);
        if (existingStudent == null) {
            return ResponseEntity.notFound().build();
        }
        
        student.setId(id);
        student.setUpdatedAt(LocalDateTime.now());
        student.setUpdatedBy("admin"); // 实际应用中应该从登录用户获取
        student.setCreatedAt(existingStudent.getCreatedAt());
        student.setCreatedBy(existingStudent.getCreatedBy());
        
        boolean success = studentService.updateById(student);
        if (success) {
            return ResponseEntity.ok(student);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除学生
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean exists = studentService.getById(id) != null;
        if (!exists) {
            return ResponseEntity.notFound().build();
        }
        
        boolean success = studentService.removeById(id);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }


    @PostMapping("/queryAllGrade")
    public ResponseEntity<IPage<StudentGradeVO>> queryAllGrade(@RequestBody PageQueryParam<StudentQueryReq> studentQueryReq) {
        IPage<StudentGradeVO> students = studentService.queryAllGrade(studentQueryReq);
        log.info("{}",students);
        return ResponseEntity.ok(students);
    }


}

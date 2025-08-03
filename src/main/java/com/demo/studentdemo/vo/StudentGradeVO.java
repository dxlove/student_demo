package com.demo.studentdemo.vo;

import lombok.Data;

@Data
public class StudentGradeVO {
    private String studentId;
    private String studentName;
    private String department;
    private String courseCode;
    private String courseName;
    private Integer credit;
    private String courseType;
    private String semester;
    private Double score;
    private String gradeLetter;
}

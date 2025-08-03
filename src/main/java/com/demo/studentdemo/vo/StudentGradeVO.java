package com.demo.studentdemo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class StudentGradeVO {
    @Schema(description = "学号", example = "202103210001")
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

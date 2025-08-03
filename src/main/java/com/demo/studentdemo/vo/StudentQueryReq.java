package com.demo.studentdemo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class StudentQueryReq {
    @Schema(description = "学号", example = "202103210001", required = false)
    private String studentId;
    @Schema(description = "课程名称", example = "计算机网络", required = false)
    private String courseName;
}

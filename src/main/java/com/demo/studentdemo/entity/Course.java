package com.demo.studentdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 课程信息
 * </p>
 *
 * @author admin
 * @since 2025-08-03
 */
@Getter
@Setter
@ToString
@TableName("t_course")
@Schema(name = "Course", description = "课程信息")
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增主键ID
     */
    @Schema(description = "自增主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 课程代码
     */
    @Schema(description = "课程代码")
    private String courseCode;

    /**
     * 课程名称
     */
    @Schema(description = "课程名称")
    private String courseName;

    /**
     * 学分(1-6)
     */
    @Schema(description = "学分(1-6)")
    private Byte credit;

    /**
     * 课程类型
     */
    @Schema(description = "课程类型")
    private String courseType;

    /**
     * 开课院系名称
     */
    @Schema(description = "开课院系名称")
    private String deptName;

    /**
     * 课程描述
     */
    @Schema(description = "课程描述")
    private String courseDesc;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String createdBy;

    /**
     * 最后更新人
     */
    @Schema(description = "最后更新人")
    private String updatedBy;
}

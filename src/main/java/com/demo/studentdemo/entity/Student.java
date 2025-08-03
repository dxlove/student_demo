package com.demo.studentdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;



/**
 * <p>
 * 学生基本信息
 * </p>
 *
 * @author admin
 * @since 2025-07-30
 */
@Getter
@Setter
@ToString
@TableName("t_student")
@ApiModel(value = "Student对象", description = "学生基本信息")
public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增主键ID
     */
    @ApiModelProperty("自增主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 学号
     */
    @ApiModelProperty("学号")
    private String studentId;

    /**
     * 身份证号
     */
    @ApiModelProperty("身份证号")
    private String idCard;

    /**
     * 姓名
     */
    @ApiModelProperty("姓名")
    private String name;

    /**
     * 所属院系名称
     */
    @ApiModelProperty("所属院系名称")
    private String deptName;

    /**
     * 入学年份
     */
    @ApiModelProperty("入学年份")
    private LocalDate enrollmentYear;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    @ApiModelProperty("最后更新时间")
    private LocalDateTime updatedAt;

    /**
     * 创建人
     */
    @ApiModelProperty("创建人")
    private String createdBy;

    /**
     * 最后更新人
     */
    @ApiModelProperty("最后更新人")
    private String updatedBy;
}

package com.demo.studentdemo.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class PageQueryParam<T> {
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    @NotNull(message = "每页数量不能为空")
    @Range(min = 1, max = 200, message = "每页数量在1-200之间")
    private Integer pageSize = 10;

    private T condition; // 通用查询条件
}


package com.sparkseries.common.util.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页响应类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应类")
public class PageResult<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "查询结果总数")
    private long total;

    @Schema(description = "查询结果集合")
    private List<T> list;
}

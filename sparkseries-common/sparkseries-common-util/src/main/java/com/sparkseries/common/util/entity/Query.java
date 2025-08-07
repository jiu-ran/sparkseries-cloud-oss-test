package com.sparkseries.common.util.entity;

import com.sparkseries.common.util.validator.Groups;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用查询类
 */
@Data
@Schema(description = "通用查询类")
public class Query implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "页码不能为空", groups = {Groups.Select.class})
    @Range(min = 1, message = "页码最小为 1")
    @Schema(description = "当前页码")
    private Integer current;

    @NotNull(message = "页面大小不能为空", groups = {Groups.Select.class})
    @Range(min = 1, max = 1000, message = "页面大小范围: 1-1000")
    @Schema(description = "当前页面大小")
    private Integer size;

    @Schema(description = "排序字段")
    private String sortField;

    @Schema(description = "是否升序")
    private boolean asc;
}

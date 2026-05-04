package com.lablink.cloudmind.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<T> records;

    private Long total;

    private Long pageNo;

    private Long pageSize;

    public static <T> PageResult<T> of(List<T> records, Long total, Long pageNo, Long pageSize) {
        return new PageResult<>(records, total, pageNo, pageSize);
    }
}
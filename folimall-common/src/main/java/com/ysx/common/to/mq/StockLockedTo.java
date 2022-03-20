package com.ysx.common.to.mq;

import lombok.Data;

import java.util.List;

@Data
public class StockLockedTo {

    private Long id;//库存单id
    private StockDetailTo detail;//工作单所有的id
}

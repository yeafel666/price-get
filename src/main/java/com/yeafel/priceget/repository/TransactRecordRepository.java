package com.yeafel.priceget.repository;

import com.yeafel.priceget.entity.TransactRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

/**
 * @author Yeafel
 * 2021/3/14 1:58
 * Do or Die,To be a better man!
 */
public interface TransactRecordRepository extends JpaRepository<TransactRecord,Integer> {

    /**
     * 根据商品Id和交易时间查询
     * @param goodsId
     * @param transactTime
     * @return
     */
    List<TransactRecord> findByGoodsIdAndTransactTime(Long goodsId, Date transactTime);


    /**
     * 根据igxId查询唯一记录
     * @param igxId
     * @return
     */
    TransactRecord findByIgxId(Long igxId);
}

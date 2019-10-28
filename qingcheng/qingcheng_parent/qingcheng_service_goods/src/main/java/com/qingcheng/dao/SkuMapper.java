package com.qingcheng.dao;

import com.qingcheng.pojo.goods.Sku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

public interface SkuMapper extends Mapper<Sku> {

     /**
      * @UpdateUser:     heiye
      * @UpdateRemark:   扣减库存
      */
     @Update("update tb_sku set num=num-#{num} where id = #{skuId}")
      void deductionStock(@Param("skuId") String skuId,@Param("num")Integer num);


      /**
       * @UpdateUser:     heiye
       * @UpdateRemark:   增加销量
       */
      @Update("update tb_sku set sale_num = sale_num+#{saleNUm} where id = #{skuId}")
      void addSaleNum(@Param("skuId") String skuId,@Param("saleNUm")Integer saleNUm);
}

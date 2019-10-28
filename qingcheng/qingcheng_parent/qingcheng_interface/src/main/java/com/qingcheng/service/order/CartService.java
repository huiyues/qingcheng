package com.qingcheng.service.order;

import java.util.List;
import java.util.Map;

public interface CartService {


    List<Map<String,Object>> findCartList(String username);

    void addItem(String username,String skuId,Integer num);

    void updateChecked(String username, String skuId, Boolean checked);

    void deleteCheckedCart(String username);

    int preferential(String username);

    List<Map<String,Object>> findNewOrderItemList(String username);
}

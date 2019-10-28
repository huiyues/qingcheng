package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.OrderItemMapper;
import com.qingcheng.dao.OrderMapper;
import com.qingcheng.dao.ReturnOrderItemMapper;
import com.qingcheng.dao.ReturnOrderMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.pojo.order.ReturnOrder;
import com.qingcheng.pojo.order.ReturnOrderItem;
import com.qingcheng.service.order.ReturnOrderItemService;
import com.qingcheng.utils.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ReturnOrderItemServiceImpl implements ReturnOrderItemService {

    @Autowired
    private ReturnOrderItemMapper returnOrderItemMapper;


    @Autowired
    private ReturnOrderMapper returnOrderMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private ReturnOrderItemMapper orderItemMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMappers;

    /**
     * 返回全部记录
     *
     * @return
     */
    public List<ReturnOrderItem> findAll() {
        return returnOrderItemMapper.selectAll();
    }

    /**
     * 分页查询
     *
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<ReturnOrderItem> findPage(int page, int size) {
        PageHelper.startPage(page, size);
        Page<ReturnOrderItem> returnOrderItems = (Page<ReturnOrderItem>) returnOrderItemMapper.selectAll();
        return new PageResult<ReturnOrderItem>(returnOrderItems.getTotal(), returnOrderItems.getResult());
    }

    /**
     * 条件查询
     *
     * @param searchMap 查询条件
     * @return
     */
    public List<ReturnOrderItem> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return returnOrderItemMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     *
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<ReturnOrderItem> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page, size);
        Example example = createExample(searchMap);
        Page<ReturnOrderItem> returnOrderItems = (Page<ReturnOrderItem>) returnOrderItemMapper.selectByExample(example);
        return new PageResult<ReturnOrderItem>(returnOrderItems.getTotal(), returnOrderItems.getResult());
    }

    /**
     * 根据Id查询
     *
     * @param id
     * @return
     */
    public ReturnOrderItem findById(Long id) {
        return returnOrderItemMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     *
     * @param returnOrderItem
     */
    public void add(ReturnOrderItem returnOrderItem) {
        returnOrderItemMapper.insert(returnOrderItem);
    }

    /**
     * 修改
     *
     * @param returnOrderItem
     */
    public void update(ReturnOrderItem returnOrderItem) {
        returnOrderItemMapper.updateByPrimaryKeySelective(returnOrderItem);
    }

    /**
     * 删除
     *
     * @param id
     */
    public void delete(Long id) {
        returnOrderItemMapper.deleteByPrimaryKey(id);
    }


    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 退款
     */
    @Override
    public void updateRefund(String id, String money, String adminId) {
        //通过退货退款明细id对象
        ReturnOrder returnOrder = returnOrderMapper.selectByPrimaryKey(id);
        returnOrder.setStatus("1");//修改退款申请状态为同意申请
        OrderItem orderItem = orderItemMappers.selectByPrimaryKey(returnOrder.getOrderId());

        if (returnOrder == null) {
            throw new RuntimeException("不存在该退款订单");
        }

        //对退货订单进行退货完成是否退款
        if ("1".equals(returnOrder.getType())) {
            orderItem.setIsReturn("2");//设置退货状态为已退货
        }

        if ("2".equals(returnOrder.getType())) {
            orderItem.setIsReturn("2");//设置退货状态为已退货
        }
        if (Integer.parseInt(money) > returnOrder.getReturnMoney() || Integer.parseInt(money) <= 0) {
            throw new RuntimeException("退款金额不合法!");
        }

        returnOrder.setReturnMoney(Integer.parseInt(money));//退款金额
        returnOrder.setAdminId(Integer.parseInt(adminId));//设置处理者
        returnOrder.setDisposeTime(new Date()); //处理日期

        //设置订单明细数据
        orderItem.setIsReturn("2");//修改订单明细为已退款


        returnOrderMapper.updateByPrimaryKey(returnOrder);
        orderItemMappers.updateByPrimaryKey(orderItem);
    }

    /**
     * @UpdateUser: heiye
     * @UpdateRemark: 退款驳回
     */
    @Override
    public void updateReject(String id, String remark, String adminId) {
        ReturnOrder returnOrder = returnOrderMapper.selectByPrimaryKey(id);
        returnOrder.setStatus("2");//修改申请状态为驳回

        if (returnOrder == null) {
            throw new RuntimeException("不存在该退款订单");
        }


      /*  if (!"2".equals(returnOrder.getType())) {
            throw new RuntimeException("不是退款订单");
        }*/


        returnOrder.setRemark(remark);//设置驳回备注
        returnOrder.setAdminId(Integer.parseInt(adminId));//设置处理者
        returnOrder.setDisposeTime(new Date()); //处理日期


        //设置退款明细数据
        Order order = orderMapper.selectByPrimaryKey(returnOrder.getOrderId());
        OrderItem orderItem = orderItemMappers.selectByPrimaryKey(order.getId());
        orderItem.setIsReturn("0");//修改为未退款

        returnOrderMapper.updateByPrimaryKey(returnOrder);
        orderItemMappers.updateByPrimaryKey(orderItem);

    }

    /**
     * 构建查询条件
     *
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap) {
        Example example = new Example(ReturnOrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        if (searchMap != null) {
            // 标题
            if (searchMap.get("title") != null && !"".equals(searchMap.get("title"))) {
                criteria.andLike("title", "%" + searchMap.get("title") + "%");
            }
            // 图片地址
            if (searchMap.get("image") != null && !"".equals(searchMap.get("image"))) {
                criteria.andLike("image", "%" + searchMap.get("image") + "%");
            }

            // 单价
            if (searchMap.get("price") != null) {
                criteria.andEqualTo("price", searchMap.get("price"));
            }
            // 数量
            if (searchMap.get("num") != null) {
                criteria.andEqualTo("num", searchMap.get("num"));
            }
            // 总金额
            if (searchMap.get("money") != null) {
                criteria.andEqualTo("money", searchMap.get("money"));
            }
            // 支付金额
            if (searchMap.get("payMoney") != null) {
                criteria.andEqualTo("payMoney", searchMap.get("payMoney"));
            }
            // 重量
            if (searchMap.get("weight") != null) {
                criteria.andEqualTo("weight", searchMap.get("weight"));
            }

        }
        return example;
    }

}

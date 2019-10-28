package com.qingcheng.pojo.order;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Table(name = "tb_data_report")
public class DataReport implements Serializable {

    @Id
    private int id;
    private Integer browseNum; //下单人数
    private Integer placeNum;  //下单
    private Integer orderNum; //订单件数
    private Integer quantityNum; //下单件数
    private Integer belowMoney;  //下单金额
    private Integer retreatMoney; //退款金额
    private Integer payPerson; //付款人数
    private Integer payOrder; //付款订单
    private Integer payNum; //付款件数
    private Integer payMoney; //付款金额
    private Integer perMoney; //客单价
    private Integer validNum;//有效订单数

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getBrowseNum() {
        return browseNum;
    }

    public void setBrowseNum(Integer browseNum) {
        this.browseNum = browseNum;
    }

    public Integer getPlaceNum() {
        return placeNum;
    }

    public void setPlaceNum(Integer placeNum) {
        this.placeNum = placeNum;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public Integer getQuantityNum() {
        return quantityNum;
    }

    public void setQuantityNum(Integer quantityNum) {
        this.quantityNum = quantityNum;
    }

    public Integer getBelowMoney() {
        return belowMoney;
    }

    public void setBelowMoney(Integer belowMoney) {
        this.belowMoney = belowMoney;
    }

    public Integer getRetreatMoney() {
        return retreatMoney;
    }

    public void setRetreatMoney(Integer retreatMoney) {
        this.retreatMoney = retreatMoney;
    }

    public Integer getPayPerson() {
        return payPerson;
    }

    public void setPayPerson(Integer payPerson) {
        this.payPerson = payPerson;
    }

    public Integer getPayOrder() {
        return payOrder;
    }

    public void setPayOrder(Integer payOrder) {
        this.payOrder = payOrder;
    }

    public Integer getPayNum() {
        return payNum;
    }

    public void setPayNum(Integer payNum) {
        this.payNum = payNum;
    }

    public Integer getPayMoney() {
        return payMoney;
    }

    public void setPayMoney(Integer payMoney) {
        this.payMoney = payMoney;
    }

    public Integer getPerMoney() {
        return perMoney;
    }

    public void setPerMoney(Integer perMoney) {
        this.perMoney = perMoney;
    }

    public Integer getValidNum() {
        return validNum;
    }

    public void setValidNum(Integer validNum) {
        this.validNum = validNum;
    }
}

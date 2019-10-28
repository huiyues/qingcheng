package com.qingcheng.pojo.system;

import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "tb_resource_menu")
public class ResourceAndMenu {

    @Id
    private Integer resourceId;


    @Id
    private Integer menuId;

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getMenuId() {
        return menuId;
    }

    public void setMenuId(Integer menuId) {
        this.menuId = menuId;
    }
}

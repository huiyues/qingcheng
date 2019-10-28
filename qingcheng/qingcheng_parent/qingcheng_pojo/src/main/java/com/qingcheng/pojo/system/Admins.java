package com.qingcheng.pojo.system;

import java.io.Serializable;
import java.util.List;

public class Admins implements Serializable{

    private Admin admin;

    private List<Role> roleList;

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public List<Role> getRoleList() {
        return roleList;
    }

    public void setRoleList(List<Role> roleList) {
        this.roleList = roleList;
    }
}

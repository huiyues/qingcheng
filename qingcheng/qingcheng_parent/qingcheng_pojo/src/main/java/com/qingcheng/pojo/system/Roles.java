package com.qingcheng.pojo.system;

import java.io.Serializable;
import java.util.List;

public class Roles implements Serializable {

    private Role role;

    private List<Resource> resourceList;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Resource> getResourceList() {
        return resourceList;
    }

    public void setResourceList(List<Resource> resourceList) {
        this.resourceList = resourceList;
    }
}

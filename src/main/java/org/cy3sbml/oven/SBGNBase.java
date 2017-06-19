package org.cy3sbml.oven;

import org.sbml.jsbml.AbstractSBase;


public class SBGNBase extends AbstractSBase {
    private String role;

    @Override
    public AbstractSBase clone() {
        return null;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public boolean isSetRole(){
        return role != null;
    }
}

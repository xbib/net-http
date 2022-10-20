package org.xbib.net.http.server.auth;

import org.xbib.net.Attributes;
import org.xbib.net.UserProfile;
import org.xbib.net.http.server.BaseAttributes;

import java.util.ArrayList;
import java.util.List;

public class BaseUserProfile implements UserProfile {

    private final Attributes attributes;

    private final Attributes effectiveAttributes;

    private final List<String> roles;

    private final List<String> effectiveRoles;

    private final List<String> permissions;

    private final List<String> effectivePermissions;

    private String uid;

    private String euid;

    private String name;

    private boolean isRemembered;

    private boolean isLoggedIn;

    public BaseUserProfile() {
        this.attributes = new BaseAttributes();
        this.effectiveAttributes = new BaseAttributes();
        this.roles = new ArrayList<>();
        this.effectiveRoles = new ArrayList<>();
        this.permissions = new ArrayList<>();
        this.effectivePermissions = new ArrayList<>();
    }

    @Override
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    @Override
    public void setUserId(String uid) {
        this.uid = uid;
        this.isLoggedIn = uid != null;
    }

    @Override
    public String getUserId() {
        return uid;
    }

    @Override
    public void setEffectiveUserId(String euid) {
        this.euid = euid;
    }

    @Override
    public String getEffectiveUserId() {
        return euid;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addRole(String role) {
        roles.add(role);
    }

    @Override
    public void addEffectiveRole(String role) {
        effectiveRoles.add(role);
    }

    @Override
    public List<String> getRoles() {
        return roles;
    }

    @Override
    public List<String> getEffectiveRoles() {
        return effectiveRoles;
    }

    @Override
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    @Override
    public boolean hasEffectiveRole(String role) {
        return effectiveRoles.contains(role);
    }

    @Override
    public boolean hasAccess(String requireAnyRole, String requireAllRoles) {
        boolean access = true;
        if (!requireAnyRole.isEmpty()) {
            String[] expectedRoles = requireAnyRole.split(",");
            if (!hasAnyRole(expectedRoles)) {
                access = false;
            }
        } else if (!requireAllRoles.isEmpty()) {
            String[] expectedRoles = requireAllRoles.split(",");
            if (!hasAllRoles(expectedRoles)) {
                access = false;
            }
        }
        return access;
    }

    @Override
    public boolean hasAnyRole(String[] expectedRoles) {
        if (expectedRoles == null || expectedRoles.length == 0) {
            return true;
        }
        for (final String role : expectedRoles) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnyEffectiveRole(String[] expectedRoles) {
        if (expectedRoles == null || expectedRoles.length == 0) {
            return true;
        }
        for (final String role : expectedRoles) {
            if (effectiveRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAllRoles(String[] expectedRoles) {
        if (expectedRoles == null || expectedRoles.length == 0) {
            return true;
        }
        for (String role : expectedRoles) {
            if (!roles.contains(role)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasAllEffectiveRoles(String[] expectedRoles) {
        if (expectedRoles == null || expectedRoles.length == 0) {
            return true;
        }
        for (String role : expectedRoles) {
            if (!effectiveRoles.contains(role)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void addPermission(String permission) {
        permissions.add(permission);
    }

    @Override
    public void removePermission(String permission) {
        permissions.remove(permission);
    }

    @Override
    public void setRemembered(boolean remembered) {
        this.isRemembered = remembered;
    }

    @Override
    public Attributes attributes() {
        return attributes;
    }

    @Override
    public Attributes effectiveAttributes() {
        return effectiveAttributes;
    }

    @Override
    public List<String> getPermissions() {
        return permissions;
    }

    @Override
    public List<String> getEffectivePermissions() {
        return effectivePermissions;
    }

    @Override
    public boolean isRemembered() {
        return isRemembered;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("uid=").append(uid)
                .append(",roles=").append(roles)
                .append(",permissons=").append(permissions)
                .append(",attributes=").append(attributes)
                .append(",euid=").append(euid)
                .append(",eroles=").append(effectiveRoles)
                .append(",epermissions=").append(effectivePermissions)
                .append(",eattributes=").append(effectiveAttributes);
        return sb.toString();
    }
}

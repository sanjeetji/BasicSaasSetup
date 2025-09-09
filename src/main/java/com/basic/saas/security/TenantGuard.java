package com.basic.saas.security;

import com.basic.saas.utils.globalExceptionHandller.CustomBusinessException;
import com.basic.saas.utils.globalExceptionHandller.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.Objects;

import static com.basic.saas.security.AuthContext.clientIdOrNull;
import static com.basic.saas.security.AuthContext.isSuperAdmin;

@Component
public class TenantGuard {
    /** Allow if SUPER_ADMIN or same tenant, else 403 */
    public void sameTenantOrSuper(Long targetClientId) {
        if (isSuperAdmin()) return;
        Long mine = clientIdOrNull();
        if (mine != null && Objects.equals(mine, targetClientId)) return;
        throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, "Cross-tenant access denied");
    }
}

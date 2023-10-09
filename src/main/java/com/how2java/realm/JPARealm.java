package com.how2java.realm;

import com.how2java.pojo.User;
import com.how2java.service.UserService;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;

public class JPARealm extends AuthorizingRealm {
    @Autowired
    private UserService userService;

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SimpleAuthorizationInfo s = new SimpleAuthorizationInfo();
        return s;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String userName = token.getPrincipal().toString(); // 通过token拿到用户名
        User user = userService.getByName(userName);  // 通过用户名拿到用户对象
        String passwordInDB = user.getPassword();  // 通过用户对象拿到数据库中已经加密的密码
        String salt = user.getSalt();  // 通过用户对象拿到数据库中的盐

        // 把用户名、加密的密码和盐这些参数带进去，能够自动验证用户登录的信息是否是正确的
        SimpleAuthenticationInfo authenticationInfo = new SimpleAuthenticationInfo(userName, passwordInDB,
                ByteSource.Util.bytes(salt), getName());
        return authenticationInfo;
    }
}

package com.taobao.rigel.rap.account.service.impl;

import com.taobao.rigel.rap.account.bo.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Base64Utils;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhangsihao on 2017/6/2.
 */
public class AccountMgrLdapImpl extends AccountMgrImpl {
    private static final Logger logger = LogManager.getLogger(AccountMgrLdapImpl.class);

    @Value("${ldap.url}")
    private String ldapUrl;
    @Value("${ldap.account}")
    private String ldapAccount;
    @Value("${ldap.password}")
    private String ldapPassword;
    @Value("${ldap.base_dn}")
    private String baseDN;
    @Value("${ldap.user_dn}")
    private String userDN;
    @Value("${ldap.user_objectclass}")
    private String userObjClass;
    private LdapContext ldapContext;

    private Properties props;

    public AccountMgrLdapImpl() {
        props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            logger.warn("Failed to load configs.");
        }
    }

    private LdapContext connectLdap() {
        if (ldapContext == null) {
            synchronized (this) {
                if (ldapContext == null) {
                    logger.info("Connecting to ldap server.");
                    Hashtable env = new Hashtable();
                    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                    env.put(Context.PROVIDER_URL, getLdapUrl());
                    env.put(Context.SECURITY_AUTHENTICATION, "simple");
                    env.put(Context.SECURITY_PRINCIPAL, getLdapAccount());
                    env.put(Context.SECURITY_CREDENTIALS, getLdapPassword());
                    env.put("java.naming.referral", "follow");
                    try {
                        ldapContext = new InitialLdapContext(env, null);
                    } catch (NamingException e) {
                        throw new RuntimeException("Failed to connect to ldap server.");
                    }
                }
            }
        }
        return ldapContext;
    }

    @Override
    public boolean validate(String account, String password) {
        boolean result = super.validate(account, password);
        if (!result) {
            logger.info("User not found in rap database, try to find in LDAP.");
            connectLdap();
            if (ldapContext == null) {
                logger.warn("There's no LDAP context.");
                return result;
            }
            String filter = String.format("(&(objectClass=%s)(cn=%s))", userObjClass, account);
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            try {
                logger.info("Searching user in LDAP.");
                NamingEnumeration<SearchResult> answer = ldapContext.search(StringUtils.join(new String[]{userDN, baseDN}, ","), filter, searchControls);
                while (answer.hasMore()) {
                    SearchResult record = answer.next();
                    logger.info("Got {}", record);
                    Attributes attrs = record.getAttributes();
                    if (checkLdapPwd(attrs.get("userpassword"), password)) {
                        // Pass
                        User user = getUser(account);
                        if (user == null) {
                            user = new User();
                            user.setAccount(account);
                            user.setPassword(password);
                            user.setEmail(attrs.get("mail").get().toString());
                            user.setName(attrs.get("displayname").get().toString());
                            addUser(user);
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
                return false;
            } catch (NamingException e) {
                return result;
            }
        } else {
            return true;
        }
    }

    private static final Pattern PAT_PWDSTR = Pattern.compile("^\\{([a-zA-Z0-9]+)\\}([a-zA-Z0-9]+)$");

    private boolean checkLdapPwd(Attribute attr, String password) {
        try {
            String pwdStr = new String((byte[])attr.get());
            Matcher matcher = PAT_PWDSTR.matcher(pwdStr);
            if (matcher.matches()) {
                String method = matcher.group(1);
                String encodedPwd = matcher.group(2);
                String pwd = DatatypeConverter.printHexBinary(Base64Utils.decodeFromString(encodedPwd)).toLowerCase();
                switch (method.toLowerCase()) {
                    case "ssha":
                    case "sha":
                        return verifySHA1(pwd, password);
                    case "md5":
                        return pwd.equals(com.taobao.rigel.rap.common.utils.StringUtils.getMD5(pwd).toLowerCase());
                    default:
                        logger.warn("Unknown password hash method.");
                        break;
                }
            }
        } catch (NamingException e) {
        }
        return false;
    }

    private boolean verifySHA1(String pwd, String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] ldappw = DatatypeConverter.parseHexBinary(pwd);
            byte[] shacode;
            byte[] salt;
            if (ldappw.length <= 20) {
                shacode = ldappw;
                salt = new byte[0];
            } else {
                shacode = new byte[20];
                salt = new byte[ldappw.length - 20];
                System.arraycopy(ldappw, 0, shacode, 0, 20);
                System.arraycopy(ldappw, 20, salt, 0, salt.length);
            }
            md.update(str.getBytes());
            md.update(salt);
            byte[] strbyte = md.digest();
            return MessageDigest.isEqual(shacode, strbyte);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }

    }

    public String getLdapUrl() {
        return props.getProperty("ldap.url");
    }

    public void setLdapUrl(String ldapUrl) {
        this.ldapUrl = ldapUrl;
    }

    public String getLdapAccount() {
        return props.getProperty("ldap.account");
    }

    public void setLdapAccount(String ldapAccount) {
        this.ldapAccount = ldapAccount;
    }

    public String getLdapPassword() {
        return props.getProperty("ldap.password");
    }

    public void setLdapPassword(String ldapPassword) {
        this.ldapPassword = ldapPassword;
    }

    public String getBaseDN() {
        return props.getProperty("ldap.base_dn");
    }

    public void setBaseDN(String baseDN) {
        this.baseDN = baseDN;
    }

    public String getUserDN() {
        return props.getProperty("ldap.user_dn");
    }

    public void setUserDN(String userDN) {
        this.userDN = userDN;
    }

    public String getUserObjClass() {
        return props.getProperty("ldap.user_objectclass");
    }

    public void setUserObjClass(String userObjClass) {
        this.userObjClass = userObjClass;
    }
}

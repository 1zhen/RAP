#!/bin/bash

cfgset()
{
    sed '/^'"$1"'=/{ s~^.*$~'"$1"'='"$2"'~g }' -i config.properties
}

if [ -z $RAP_JDBC_URL ]; then
    RAP_JDBC_URL="jdbc:mysql://localhost:3306/rap_db?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&noAccessToProcedureBodies=true"
fi

if [ -z $RAP_JDBC_USER ]; then
    RAP_JDBC_USER=root
fi

if [ -z $RAP_JDBC_PASS ]; then
    RAP_JDBC_PASS=password
fi

if [ -z $RAP_REDIS_HOST ]; then
    RAP_REDIS_HOST=localhost
fi

if [ -z $RAP_REDIS_PORT ]; then
    RAP_REDIS_PORT=6379
fi

if [ -z $RAP_REDIS_PASSWORD ]; then
    RAP_REDIS_PASSWORD=
fi

if [ -z $RAP_LDAP_ENABLED ]; then
    RAP_LDAP_ENABLED=false
fi

if [ "$RAP_LDAP_ENABLED" == "true" ]; then
    cfgset rap.system.account_mgr_class com.taobao.rigel.rap.account.service.impl.AccountMgrLdapImpl
else
    cfgset rap.system.account_mgr_class com.taobao.rigel.rap.account.service.impl.AccountMgrImpl
fi

if [ -z $RAP_LDAP_URL ]; then
    RAP_LDAP_URL="ldap://localhost:389"
fi

if [ -z $RAP_LDAP_ACCOUNT ]; then
    RAP_LDAP_ACCOUNT=
fi

if [ -z $RAP_LDAP_PASSWORD ]; then
    RAP_LDAP_PASSWORD=
fi

if [ -z $RAP_LDAP_BASE_DN ]; then
    RAP_LDAP_BASE_DN=
fi

if [ -z $RAP_LDAP_USER_DN ]; then
    RAP_LDAP_USER_DN=
fi

if [ -z $RAP_LDAP_USER_OBJECTCLASS ]; then
    RAP_LDAP_USER_OBJECTCLASS=inetOrgPerson
fi

cfgset jdbc.url $RAP_JDBC_URL
cfgset jdbc.username $RAP_JDBC_USER
cfgset jdbc.password $RAP_JDBC_PASS
cfgset redis.host $RAP_REDIS_HOST
cfgset redis.port $RAP_REDIS_PORT
cfgset redis.password $RAP_REDIS_PASSWORD
cfgset ldap.url $RAP_LDAP_URL
cfgset ldap.account $RAP_LDAP_ACCOUNT
cfgset ldap.password $RAP_LDAP_PASSWORD
cfgset ldap.base_dn $RAP_LDAP_BASE_DN
cfgset ldap.user_dn $RAP_LDAP_USER_DN
cfgset ldap.user_objectclass $RAP_LDAP_USER_OBJECTCLASS

exec mvn cargo:run
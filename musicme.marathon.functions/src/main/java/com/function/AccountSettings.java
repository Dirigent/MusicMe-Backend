package com.function;

import com.azure.cosmos.implementation.apachecommons.lang.StringUtils;

public class AccountSettings {
public static String MASTER_KEY =
    System.getProperty("ACCOUNT_KEY", 
            StringUtils.defaultString(StringUtils.trimToNull(
                    System.getenv().get("ACCOUNT_KEY")),
                    ""));

public static String HOST =
    System.getProperty("ACCOUNT_HOST",
            StringUtils.defaultString(StringUtils.trimToNull(
                    System.getenv().get("ACCOUNT_HOST")),
                    ""));
    
}
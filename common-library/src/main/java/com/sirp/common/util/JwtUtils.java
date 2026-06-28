package com.sirp.common.util;

public final class JwtUtils {

    private JwtUtils(){}

    public static String bearer(String token){

        return "Bearer " + token;

    }

}
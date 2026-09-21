package com.unla.museo.controllers.util;

public interface LinksApi {
	
    final String API_AUTH = "/api-auth/auth";
    static interface AuthEndpoints {
        final String REGISTER = API_AUTH + "/register";
        final String LOGIN = API_AUTH + "/login";
        final String LOGOUT =  API_AUTH +"/logout";
        final String ME = API_AUTH + "/me";
	}

}

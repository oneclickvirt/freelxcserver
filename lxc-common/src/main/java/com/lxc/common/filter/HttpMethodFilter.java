package com.lxc.common.filter;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpMethodFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String httpMethod = request.getMethod();

        // 合法的HTTP方法名列表
        String[] validMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT", "PATCH"};

        boolean isValid = false;
        for (String validMethod : validMethods) {
            if (validMethod.equalsIgnoreCase(httpMethod)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            // 请求的方法名非法，返回405 Method Not Allowed响应
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Invalid HTTP method: " + httpMethod);
        } else {
            // 请求的方法名合法，继续处理请求
            filterChain.doFilter(request, response);
        }
    }
}

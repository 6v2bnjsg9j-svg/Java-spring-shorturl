package com.wcy.shorturl.Interceptor;



import com.wcy.shorturl.util.CurrentHolder;
import com.wcy.shorturl.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Slf4j
@Component
public class MyInterceptor implements HandlerInterceptor {
   @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
       // 放行 CORS 预检请求（OPTIONS 不携带 token，不应被拦截）
       if ("OPTIONS".equalsIgnoreCase(request.getMethod()))
           return true;
       String uri = request.getRequestURI();//获取请求的路径
       if(uri.contains("login")||uri.contains("logout"))
           return true;
       else{
            String token = request.getHeader("token");
            if(token==null||token.equals("")){
                response.setStatus(401);
                return false;
            }
            else{
                try{
                    Map<String,Object> claims = JwtUtil.parseJWT(token);

                    Long role = (Long) claims.get("id");
                    CurrentHolder.setCurrentId(role);
                }
                catch (Exception e){
                    response.setStatus(401);
                    return false;
                }
            }
       }return  true;

    }
}

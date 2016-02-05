package org.gameon.map.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

@WebFilter(
        filterName = "corsFilter",
        urlPatterns = {"/*"}
          )
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String id = request.getParameter("id");

        // TODO: a bunch of verification...
        if ( id == null )
            id = ((HttpServletRequest)request).getHeader("gameon-id");
            if ( id == null ) {
                id = "game-on.org-provisional";
            }
        request.setAttribute("player.id", id);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}

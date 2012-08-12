package com.alibaba.druid.support.http;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.alibaba.druid.filter.stat.StatFilterContext;
import com.alibaba.druid.filter.stat.StatFilterContextListenerAdapter;
import com.alibaba.druid.support.http.stat.WebAppStat;
import com.alibaba.druid.support.http.stat.WebAppStatManager;
import com.alibaba.druid.support.http.stat.WebURIStat;

public class WebStatFilter implements Filter {

    private WebAppStat                   webAppStat                = null;
    private WebStatFilterContextListener statFilterContextListener = new WebStatFilterContextListener();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                             ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        final String requestURI = getRequestURI(httpRequest);

        if (isExclusion(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        long startNano = System.nanoTime();
        webAppStat.beforeInvoke(requestURI);

        try {
            chain.doFilter(request, response);
        } finally {
            long nanoSpan = System.nanoTime() - startNano;

            webAppStat.afterInvoke(nanoSpan);
        }
    }

    public boolean isExclusion(String uri) {
        return false;
    }

    public String getRequestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        config.getServletContext().getContextPath();

        StatFilterContext.getInstance().addContextListener(statFilterContextListener);

        webAppStat = new WebAppStat();

        WebAppStatManager.getInstance().addWebAppStatSet(webAppStat);
    }

    @Override
    public void destroy() {
        StatFilterContext.getInstance().removeContextListener(statFilterContextListener);

        if (webAppStat != null) {
            WebAppStatManager.getInstance().remove(webAppStat);
        }
    }

    class WebStatFilterContextListener extends StatFilterContextListenerAdapter {

        @Override
        public void addUpdateCount(int updateCount) {
            WebURIStat stat = WebURIStat.current();
            if (stat != null) {
                stat.addJdbcUpdateCount(updateCount);
            }
        }

        @Override
        public void addFetchRowCount(int fetchRowCount) {
            WebURIStat stat = WebURIStat.current();
            if (stat != null) {
                stat.addJdbcFetchRowCount(fetchRowCount);
            }
        }

        @Override
        public void executeBefore(String sql, boolean inTransaction) {
            WebURIStat stat = WebURIStat.current();
            if (stat != null) {
                stat.incrementJdbcExecuteCount();
            }
        }

        @Override
        public void executeAfter(String sql, long nanoSpan, Throwable error) {

        }
        
        @Override
        public void commit() {
            WebURIStat stat = WebURIStat.current();
            if (stat != null) {
                stat.incrementJdbcCommitCount();
            }
        }

        @Override
        public void rollback() {
            WebURIStat stat = WebURIStat.current();
            if (stat != null) {
                stat.incrementJdbcRollbackCount();
            }
        }
    }
}

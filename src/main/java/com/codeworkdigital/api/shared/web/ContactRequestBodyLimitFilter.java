package com.codeworkdigital.api.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class ContactRequestBodyLimitFilter extends OncePerRequestFilter {

    private static final String CONTACT_SUBMISSIONS_PATH = "/api/v1/contact-submissions";
    private static final String API_PATH_PREFIX = "/api/";
    private static final int BUFFER_SIZE = 8192;

    private final ApiWebProperties properties;
    private final HandlerExceptionResolver exceptionResolver;

    public ContactRequestBodyLimitFilter(
            ApiWebProperties properties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.properties = properties;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith(API_PATH_PREFIX)) {
            addDefensiveHeaders(response);
        }

        if (!shouldLimit(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = properties.maxContactRequestBytes();
        long contentLength = request.getContentLengthLong();
        if (contentLength > limit) {
            reject(request, response);
            return;
        }

        byte[] body = readAtMost(request, limit + 1);
        if (body.length > limit) {
            reject(request, response);
            return;
        }

        filterChain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private boolean shouldLimit(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && CONTACT_SUBMISSIONS_PATH.equals(request.getRequestURI());
    }

    private void addDefensiveHeaders(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
    }

    private byte[] readAtMost(HttpServletRequest request, int maxBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, BUFFER_SIZE));
        byte[] buffer = new byte[Math.min(maxBytes, BUFFER_SIZE)];
        int total = 0;
        int read;
        ServletInputStream input = request.getInputStream();
        while (total < maxBytes
                && (read = input.read(buffer, 0, Math.min(buffer.length, maxBytes - total))) != -1) {
            output.write(buffer, 0, read);
            total += read;
        }
        return output.toByteArray();
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) {
        exceptionResolver.resolveException(request, response, null, new RequestBodyTooLargeException());
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body.clone();
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedBodyInputStream(body);
        }

        @Override
        public BufferedReader getReader() {
            Charset charset = getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(getCharacterEncoding());
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }

    private static final class CachedBodyInputStream extends ServletInputStream {

        private final ByteArrayInputStream input;

        private CachedBodyInputStream(byte[] body) {
            this.input = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return input.read();
        }

        @Override
        public boolean isFinished() {
            return input.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            if (readListener == null) {
                throw new IllegalArgumentException("readListener is required");
            }
            try {
                if (!isFinished()) {
                    readListener.onDataAvailable();
                }
                if (isFinished()) {
                    readListener.onAllDataRead();
                }
            } catch (IOException exception) {
                readListener.onError(exception);
            }
        }
    }
}

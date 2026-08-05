package com.codeworkdigital.api.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

class ContactRequestBodyLimitFilterTest {

    private static final int LIMIT = 16;

    @Test
    void doesNotReadBodyOutsideContactRoute() throws Exception {
        RecordingResolver resolver = new RecordingResolver();
        ContactRequestBodyLimitFilter filter = filter(resolver);
        ThrowingBodyRequest request = new ThrowingBodyRequest("POST", "/api/v1/other", 100);
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain(false);

        filter.doFilter(request, response, chain);

        assertThat(chain.called).isTrue();
        assertThat(resolver.exceptions).isEmpty();
    }

    @Test
    void doesNotApplyLimitToOptions() throws Exception {
        RecordingResolver resolver = new RecordingResolver();
        ContactRequestBodyLimitFilter filter = filter(resolver);
        ThrowingBodyRequest request = new ThrowingBodyRequest("OPTIONS", "/api/v1/contact-submissions", 100);
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain(false);

        filter.doFilter(request, response, chain);

        assertThat(chain.called).isTrue();
        assertThat(resolver.exceptions).isEmpty();
    }

    @Test
    void rejectsKnownContentLengthAboveLimitBeforeReadingBody() throws Exception {
        RecordingResolver resolver = new RecordingResolver();
        ContactRequestBodyLimitFilter filter = filter(resolver);
        ThrowingBodyRequest request = new ThrowingBodyRequest("POST", "/api/v1/contact-submissions", LIMIT + 1);
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain(true);

        filter.doFilter(request, response, chain);

        assertThat(chain.called).isFalse();
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(resolver.exceptions).singleElement().isInstanceOf(RequestBodyTooLargeException.class);
    }

    @Test
    void bodyInsideLimitReachesChainComplete() throws Exception {
        byte[] body = "small body".getBytes(StandardCharsets.UTF_8);
        RecordingChain chain = filterBody(body.length).apply(body);

        assertThat(chain.body).isEqualTo(body);
    }

    @Test
    void bodyExactlyAtLimitIsAccepted() throws Exception {
        byte[] body = "a".repeat(LIMIT).getBytes(StandardCharsets.UTF_8);
        RecordingChain chain = filterBody(body.length).apply(body);

        assertThat(chain.body).isEqualTo(body);
    }

    @Test
    void bodyOneByteAboveLimitIsRejected() throws Exception {
        RecordingResolver resolver = new RecordingResolver();
        ContactRequestBodyLimitFilter filter = filter(resolver);
        MockHttpServletRequest request = contactRequest("a".repeat(LIMIT + 1).getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain(true);

        filter.doFilter(request, response, chain);

        assertThat(chain.called).isFalse();
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(resolver.exceptions).singleElement().isInstanceOf(RequestBodyTooLargeException.class);
    }

    @Test
    void rejectsOversizedBodyWithUnknownContentLength() throws Exception {
        RecordingResolver resolver = new RecordingResolver();
        ContactRequestBodyLimitFilter filter = filter(resolver);
        MockHttpServletRequest request = new UnknownLengthRequest("a".repeat(LIMIT + 1).getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain(true);

        filter.doFilter(request, response, chain);

        assertThat(chain.called).isFalse();
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(resolver.exceptions).singleElement().isInstanceOf(RequestBodyTooLargeException.class);
    }

    @Test
    void emptyBodyContinuesToMvc() throws Exception {
        RecordingChain chain = filterBody(0).apply(new byte[0]);

        assertThat(chain.body).isEmpty();
    }

    @Test
    void exceptionMessageDoesNotContainBodyOrSize() {
        RequestBodyTooLargeException exception = new RequestBodyTooLargeException();

        assertThat(exception.getMessage())
                .doesNotContain("17")
                .doesNotContain("secret")
                .doesNotContain("{")
                .doesNotContain("}");
    }

    private BodyFilter filterBody(long contentLength) {
        return body -> {
            RecordingResolver resolver = new RecordingResolver();
            ContactRequestBodyLimitFilter filter = filter(resolver);
            MockHttpServletRequest request = contentLength < 0
                    ? new UnknownLengthRequest(body)
                    : contactRequest(body);
            MockHttpServletResponse response = new MockHttpServletResponse();
            RecordingChain chain = new RecordingChain(true);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(resolver.exceptions).isEmpty();
            assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
            return chain;
        };
    }

    private ContactRequestBodyLimitFilter filter(HandlerExceptionResolver resolver) {
        ApiWebProperties properties = new ApiWebProperties(Set.of("http://localhost:3000"), Duration.ofHours(1), LIMIT);
        return new ContactRequestBodyLimitFilter(properties, resolver);
    }

    private MockHttpServletRequest contactRequest(byte[] body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/contact-submissions");
        request.setContent(body);
        return request;
    }

    private byte[] read(HttpServletRequest request) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8];
        int read;
        ServletInputStream input = request.getInputStream();
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private final class RecordingChain implements FilterChain {

        private final boolean readBody;
        private boolean called;
        private byte[] body;

        private RecordingChain(boolean readBody) {
            this.readBody = readBody;
        }

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
                throws IOException {
            called = true;
            if (readBody) {
                body = read((HttpServletRequest) request);
            }
        }
    }

    private static final class RecordingResolver implements HandlerExceptionResolver {

        private final List<Exception> exceptions = new ArrayList<>();

        @Override
        public ModelAndView resolveException(
                HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response,
                Object handler,
                Exception exception) {
            exceptions.add(exception);
            response.setStatus(413);
            return new ModelAndView();
        }
    }

    private static class ThrowingBodyRequest extends MockHttpServletRequest {

        private final long contentLength;

        private ThrowingBodyRequest(String method, String requestUri, long contentLength) {
            super(method, requestUri);
            this.contentLength = contentLength;
        }

        @Override
        public long getContentLengthLong() {
            return contentLength;
        }

        @Override
        public ServletInputStream getInputStream() {
            throw new AssertionError("body must not be read");
        }
    }

    private static final class UnknownLengthRequest extends MockHttpServletRequest {

        private UnknownLengthRequest(byte[] body) {
            super("POST", "/api/v1/contact-submissions");
            setContent(body);
        }

        @Override
        public long getContentLengthLong() {
            return -1;
        }
    }

    @FunctionalInterface
    private interface BodyFilter {
        RecordingChain apply(byte[] body) throws Exception;
    }
}

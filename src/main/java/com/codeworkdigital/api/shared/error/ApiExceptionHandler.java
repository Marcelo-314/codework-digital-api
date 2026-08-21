package com.codeworkdigital.api.shared.error;

import com.codeworkdigital.api.contact.api.InvalidIdempotencyKeyException;
import com.codeworkdigital.api.contact.api.UnsupportedContactValueException;
import com.codeworkdigital.api.contact.application.ContactSubmissionValidationException;
import com.codeworkdigital.api.contact.application.IdempotencyConflictException;
import com.codeworkdigital.api.contact.application.InvalidAdminPaginationException;
import com.codeworkdigital.api.processanalysis.api.UnsupportedProcessAnalysisValueException;
import com.codeworkdigital.api.processanalysis.application.InvalidProcessAnalysisModelResponseException;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationAlreadyResolvedException;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationAnswerValidationException;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationExpiredException;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationLifecycleConflictException;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationNotFoundException;
import com.codeworkdigital.api.processanalysis.application.ProcessEffortClarificationResolutionInvariantException;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisUnavailableException;
import com.codeworkdigital.api.processanalysis.application.ProcessAnalysisValidationException;
import com.codeworkdigital.api.shared.web.RequestBodyTooLargeException;
import com.codeworkdigital.api.verification.application.HumanVerificationRejectedException;
import com.codeworkdigital.api.verification.application.HumanVerificationUnavailableException;
import jakarta.validation.ConstraintViolation;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    ResponseEntity<Object> handleInvalidIdempotencyKey(InvalidIdempotencyKeyException exception, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "invalid_idempotency_key",
                "Invalid idempotency key", "The Idempotency-Key header must be a canonical UUID.", request);
    }

    @ExceptionHandler(InvalidAdminPaginationException.class)
    ResponseEntity<Object> handleInvalidAdminPagination(
            InvalidAdminPaginationException exception,
            WebRequest request) {
        ProblemDetail problem = validationProblem(request);
        problem.setProperty("errors", List.of(new ValidationError(exception.field(), "invalid")));
        return response(problem, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<Object> handleIdempotencyConflict(IdempotencyConflictException exception, WebRequest request) {
        return problem(HttpStatus.CONFLICT, "idempotency_conflict",
                "Idempotency conflict", "The Idempotency-Key was already used with a different payload.", request);
    }

    @ExceptionHandler(UnsupportedContactValueException.class)
    ResponseEntity<Object> handleUnsupportedContactValue(UnsupportedContactValueException exception, WebRequest request) {
        ProblemDetail problem = validationProblem(request);
        problem.setProperty("errors", List.of(new ValidationError(exception.field(), "unsupported_value")));
        return response(problem, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnsupportedProcessAnalysisValueException.class)
    ResponseEntity<Object> handleUnsupportedProcessAnalysisValue(
            UnsupportedProcessAnalysisValueException exception,
            WebRequest request) {
        ProblemDetail problem = validationProblem(request);
        problem.setProperty("errors", List.of(new ValidationError(exception.field(), "unsupported_value")));
        return response(problem, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ContactSubmissionValidationException.class)
    ResponseEntity<Object> handleContactSubmissionValidation(
            ContactSubmissionValidationException exception,
            WebRequest request) {
        ProblemDetail problem = validationProblem(request);
        problem.setProperty("errors", violationErrors(exception.violations()));
        return response(problem, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ProcessAnalysisValidationException.class)
    ResponseEntity<Object> handleProcessAnalysisValidation(
            ProcessAnalysisValidationException exception,
            WebRequest request) {
        ProblemDetail problem = validationProblem(request);
        problem.setProperty("errors", violationErrors(exception.violations()));
        return response(problem, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Object> handleDataAccess(DataAccessException exception, WebRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "persistence_error",
                "Persistence error", "The request could not be persisted.", request);
    }

    @ExceptionHandler(HumanVerificationRejectedException.class)
    ResponseEntity<Object> handleHumanVerificationRejected(
            HumanVerificationRejectedException exception,
            WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "human_verification_failed",
                "Human verification failed", "Human verification was not accepted. Complete the challenge again.", request);
    }

    @ExceptionHandler(HumanVerificationUnavailableException.class)
    ResponseEntity<Object> handleHumanVerificationUnavailable(
            HumanVerificationUnavailableException exception,
            WebRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "human_verification_unavailable",
                "Human verification unavailable", "Human verification is temporarily unavailable. Try again later.", request);
    }

    @ExceptionHandler(ProcessAnalysisUnavailableException.class)
    ResponseEntity<Object> handleProcessAnalysisUnavailable(
            ProcessAnalysisUnavailableException exception,
            WebRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "process_analysis_unavailable",
                "Process analysis unavailable", "Process analysis is temporarily unavailable. Try again later.", request);
    }

    @ExceptionHandler(ProcessEffortClarificationAnswerValidationException.class)
    ResponseEntity<Object> handleProcessEffortClarificationAnswerValidation(
            ProcessEffortClarificationAnswerValidationException exception,
            WebRequest request) {
        ProblemDetail problem = validationProblem(request);
        problem.setProperty("errors", List.of(new ValidationError("answers", "invalid")));
        return response(problem, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ProcessEffortClarificationNotFoundException.class)
    ResponseEntity<Object> handleProcessEffortClarificationNotFound(
            ProcessEffortClarificationNotFoundException exception,
            WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "clarification_not_found",
                "Clarification not found", "The clarification could not be found.", request);
    }

    @ExceptionHandler(ProcessEffortClarificationExpiredException.class)
    ResponseEntity<Object> handleProcessEffortClarificationExpired(
            ProcessEffortClarificationExpiredException exception,
            WebRequest request) {
        return problem(HttpStatus.GONE, "clarification_expired",
                "Clarification expired", "The clarification has expired.", request);
    }

    @ExceptionHandler(ProcessEffortClarificationAlreadyResolvedException.class)
    ResponseEntity<Object> handleProcessEffortClarificationAlreadyResolved(
            ProcessEffortClarificationAlreadyResolvedException exception,
            WebRequest request) {
        return problem(HttpStatus.CONFLICT, "clarification_already_resolved",
                "Clarification already resolved", "The clarification has already been resolved.", request);
    }

    @ExceptionHandler(ProcessEffortClarificationLifecycleConflictException.class)
    ResponseEntity<Object> handleProcessEffortClarificationLifecycleConflict(
            ProcessEffortClarificationLifecycleConflictException exception,
            WebRequest request) {
        return problem(HttpStatus.CONFLICT, "clarification_lifecycle_conflict",
                "Clarification lifecycle conflict", "The clarification is no longer active.", request);
    }

    @ExceptionHandler(ProcessEffortClarificationResolutionInvariantException.class)
    ResponseEntity<Object> handleProcessEffortClarificationResolutionInvariant(
            ProcessEffortClarificationResolutionInvariantException exception,
            WebRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "clarification_resolution_failed",
                "Clarification resolution failed", "The clarification could not be resolved.", request);
    }

    @ExceptionHandler(InvalidProcessAnalysisModelResponseException.class)
    ResponseEntity<Object> handleInvalidProcessAnalysisModelResponse(
            InvalidProcessAnalysisModelResponseException exception,
            WebRequest request) {
        return problem(HttpStatus.BAD_GATEWAY, "invalid_model_response",
                "Invalid process analysis response",
                "Process analysis returned an invalid structured result. Try again later.",
                request);
    }

    @ExceptionHandler(RequestBodyTooLargeException.class)
    ResponseEntity<Object> handleRequestBodyTooLarge(RequestBodyTooLargeException exception, WebRequest request) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "request_too_large",
                "Request body too large", "The request body exceeds the allowed size.", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = validationProblem(request);
        problem.setProperty("errors", validationErrors(exception.getBindingResult().getFieldErrors()));
        return response(problem, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "invalid_request",
                "Invalid request", "The request body is not readable JSON.", request);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
            ServletRequestBindingException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        if (exception instanceof MissingRequestHeaderException missingHeader
                && "Idempotency-Key".equalsIgnoreCase(missingHeader.getHeaderName())) {
            return problem(HttpStatus.BAD_REQUEST, "missing_idempotency_key",
                    "Missing idempotency key", "The Idempotency-Key header is required.", request);
        }
        return problem(HttpStatus.BAD_REQUEST, "invalid_request",
                "Invalid request", "The request is missing required data.", request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = baseProblem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_media_type",
                "Unsupported media type", "The endpoint accepts application/json.", request);
        return response(problem, HttpStatus.UNSUPPORTED_MEDIA_TYPE, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = baseProblem(HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed",
                "Method not allowed", "The HTTP method is not supported for this endpoint.", request);
        return response(problem, HttpStatus.METHOD_NOT_ALLOWED, headers);
    }

    private ProblemDetail validationProblem(WebRequest request) {
        return baseProblem(HttpStatus.BAD_REQUEST, "validation_failed",
                "Validation failed", "The request contains invalid fields.", request);
    }

    private List<ValidationError> validationErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(fieldError -> new ValidationError(fieldError.getField(), validationCode(fieldError.getCode())))
                .distinct()
                .sorted(Comparator.comparing(ValidationError::field).thenComparing(ValidationError::code))
                .toList();
    }

    private List<ValidationError> violationErrors(
            Iterable<? extends ConstraintViolation<?>> violations) {
        return toValidationErrors(violations);
    }

    private List<ValidationError> toValidationErrors(
            Iterable<? extends ConstraintViolation<?>> violations) {
        return java.util.stream.StreamSupport.stream(violations.spliterator(), false)
                .map(violation -> new ValidationError(
                        violation.getPropertyPath().toString(),
                        validationCode(violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())))
                .distinct()
                .sorted(Comparator.comparing(ValidationError::field).thenComparing(ValidationError::code))
                .toList();
    }

    private String validationCode(String constraintCode) {
        return switch (constraintCode) {
            case "NotBlank", "NotNull" -> "required";
            case "Email" -> "invalid_email";
            case "Size" -> "invalid_length";
            case "Pattern" -> "unsupported_value";
            default -> "invalid";
        };
    }

    private ResponseEntity<Object> problem(
            HttpStatus status,
            String code,
            String title,
            String detail,
            WebRequest request) {
        return response(baseProblem(status, code, title, detail, request), status);
    }

    private ProblemDetail baseProblem(
            HttpStatus status,
            String code,
            String title,
            String detail,
            WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:codeworkdigital:problem:" + code));
        problem.setTitle(title);
        problem.setInstance(URI.create(instancePath(request)));
        problem.setProperty("code", code);
        return problem;
    }

    private ResponseEntity<Object> response(ProblemDetail problem, HttpStatus status) {
        return response(problem, status, HttpHeaders.EMPTY);
    }

    private ResponseEntity<Object> response(ProblemDetail problem, HttpStatus status, HttpHeaders headers) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(problem, responseHeaders, status);
    }

    private String instancePath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return "/";
    }

    private record ValidationError(String field, String code) {
    }
}

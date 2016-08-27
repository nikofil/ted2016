package gr.uoa.di.exception;

import gr.uoa.di.exception.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> resourceNotFound(Exception ex) {
        return new ResponseEntity<ErrorResponse>
                (createErrorResponse(HttpStatus.NOT_FOUND, ex), HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(UserLoginException.class)
    public ResponseEntity<ErrorResponse> badRequest(Exception ex) {
        return new ResponseEntity<ErrorResponse>
                (createErrorResponse(HttpStatus.BAD_REQUEST, ex), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotValidatedException.class)
    public ResponseEntity<ErrorResponse> forbidden(Exception ex) {
        return new ResponseEntity<ErrorResponse>
                (createErrorResponse(HttpStatus.FORBIDDEN, ex), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({ UserAlreadyExistsException.class, UserAlreadyValidatedException.class })
    public ResponseEntity<ErrorResponse> conflict(Exception ex) {
        return new ResponseEntity<ErrorResponse>
                (createErrorResponse(HttpStatus.CONFLICT, ex), HttpStatus.CONFLICT);
    }

    private ErrorResponse createErrorResponse(HttpStatus status, Exception ex) {
        ErrorResponse response = new ErrorResponse();
        response.setStatus(status.value());
        response.setError(status.getReasonPhrase());
        response.setMessage(ex.getMessage());
        return response;
    }
}
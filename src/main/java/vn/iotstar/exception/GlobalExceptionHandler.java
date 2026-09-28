package vn.iotstar.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import lombok.extern.slf4j.Slf4j;

/** Gom lỗi nghiệp vụ về 1 trang lỗi thân thiện (templates/error.html). */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleBadRequest(IllegalArgumentException ex) {
        return view(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleTooLarge(MaxUploadSizeExceededException ex) {
        return view(HttpStatus.valueOf(413), "File quá lớn (tối đa 10MB).");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ModelAndView handleServerError(IllegalStateException ex) {
        log.error("Lỗi xử lý", ex);
        return view(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi khi xử lý. Vui lòng thử lại sau.");
    }

    private ModelAndView view(HttpStatus status, String message) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("message", message);
        return mav;
    }
}

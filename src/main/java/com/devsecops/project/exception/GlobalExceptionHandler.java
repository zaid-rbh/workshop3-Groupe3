package com.devsecops.project.exception;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * VULNERABILITY #17: every exception is shown to the user with its class name, its message
 * (which may contain SQL text), the root cause and the complete stack trace.
 * A secure version would log the details on the server and show a generic message.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoResourceFoundException ex, Model model) {
        fill(ex, model);
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleAll(Exception ex, Model model) {
        fill(ex, model);
        return "error";
    }

    private void fill(Exception ex, Model model) {
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);

        model.addAttribute("exceptionType", ex.getClass().getName());
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("rootCause", root.getMessage());
        model.addAttribute("trace", writer.toString());
    }
}

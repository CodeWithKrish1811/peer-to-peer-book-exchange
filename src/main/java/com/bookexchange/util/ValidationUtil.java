package com.bookexchange.util;

import com.bookexchange.exception.ValidationException;
import java.util.regex.Pattern;

/** Static input-validation helpers shared by all services. */
public final class ValidationUtil {
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern ISBN = Pattern.compile("^[0-9Xx-]{10,17}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+\\- ]{7,20}$");

    private ValidationUtil() { }

    public static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    public static String trimToNull(String s) { return isBlank(s) ? null : s.trim(); }

    public static String requireNonBlank(String value, String field) throws ValidationException {
        if (isBlank(value)) {
            throw new ValidationException(field + " is required.");
        }
        return value.trim();
    }

    /** Overload: also checks the maximum length. */
    public static String requireNonBlank(String value, String field, int max) throws ValidationException {
        String trimmed = requireNonBlank(value, field);
        requireMaxLength(trimmed, max, field);
        return trimmed;
    }

    public static void requireMaxLength(String value, int max, String field) throws ValidationException {
        if (value != null && value.length() > max) {
            throw new ValidationException(field + " must be at most " + max + " characters.");
        }
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    public static void validatePassword(String password) throws ValidationException {
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long.");
        }
        boolean letter = false;
        boolean digit = false;
        for (char ch : password.toCharArray()) {
            if (Character.isLetter(ch)) letter = true;
            if (Character.isDigit(ch)) digit = true;
        }
        if (!letter || !digit) {
            throw new ValidationException("Password must contain at least one letter and one digit.");
        }
    }

    public static void validateOptionalIsbn(String isbn) throws ValidationException {
        if (isbn != null && !ISBN.matcher(isbn).matches()) {
            throw new ValidationException("ISBN must contain 10 to 17 digits or hyphens.");
        }
    }

    public static void validateOptionalPhone(String phone) throws ValidationException {
        if (phone != null && !PHONE.matcher(phone).matches()) {
            throw new ValidationException("Phone number looks invalid.");
        }
    }

    public static int parseId(String value, String field) throws ValidationException {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ValidationException("Invalid " + field + ".");
        }
    }

    /** Returns null when the value is blank (used for optional dropdowns). */
    public static Integer parseOptionalId(String value, String field) throws ValidationException {
        return isBlank(value) ? null : Integer.valueOf(parseId(value, field));
    }
}

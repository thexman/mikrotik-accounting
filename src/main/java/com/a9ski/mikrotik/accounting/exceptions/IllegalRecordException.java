package com.a9ski.mikrotik.accounting.exceptions;

/**
 * Exception indicating an illegal record is being parsed.
 *
 * @author Kiril Arabadzhiyski
 *
 */
public class IllegalRecordException extends AccountingException {

	/**
	 *
	 */
	private static final long serialVersionUID = 4330163224819264791L;

	/**
	 * Creates new exception.
	 */
	public IllegalRecordException() {
		super();
	}

	/**
	 * Creates new exception.
	 *
	 * @param message the detail message. The detail message is saved for later
	 *                retrieval by the {@link #getMessage()} method.
	 */
	public IllegalRecordException(final String message) {
		super(message);
	}

	/**
	 * Creates new exception.
	 *
	 * @param message the detail message (which is saved for later retrieval by the
	 *                {@link #getMessage()} method).
	 * @param cause   the cause (which is saved for later retrieval by the
	 *                {@link #getCause()} method). (A {@code null} value is
	 *                permitted, and indicates that the cause is nonexistent or
	 *                unknown.)
	 */
	public IllegalRecordException(final String message, final Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates new exception.
	 *
	 * @param message            the detail message.
	 * @param cause              the cause. (A {@code null} value is permitted, and
	 *                           indicates that the cause is nonexistent or
	 *                           unknown.)
	 * @param enableSuppression  whether or not suppression is enabled or disabled
	 * @param writableStackTrace whether or not the stack trace should be writable
	 */
	public IllegalRecordException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * Creates new exception.
	 *
	 * @param cause the cause (which is saved for later retrieval by the
	 *              {@link #getCause()} method). (A {@code null} value is permitted,
	 *              and indicates that the cause is nonexistent or unknown.)
	 */
	public IllegalRecordException(final Throwable cause) {
		super(cause);
	}
}

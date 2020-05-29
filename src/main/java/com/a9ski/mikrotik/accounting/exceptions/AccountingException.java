package com.a9ski.mikrotik.accounting.exceptions;

import com.a9ski.mikrotik.exceptions.MikrotikException;

/**
 * Exception representing problem calling MikroTik accounting service.
 *
 * @author Kiril Arabadzhiyski
 *
 */
public class AccountingException extends MikrotikException {

	/**
	 *
	 */
	private static final long serialVersionUID = -4710672801312958977L;

	/**
	 * Creates new exception.
	 */
	public AccountingException() {
		super();
	}

	/**
	 * Creates new exception.
	 *
	 * @param message the detail message. The detail message is saved for later
	 *                retrieval by the {@link #getMessage()} method.
	 */
	public AccountingException(final String message) {
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
	public AccountingException(final String message, final Throwable cause) {
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
	public AccountingException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * Creates new exception.
	 *
	 * @param cause the cause (which is saved for later retrieval by the
	 *              {@link #getCause()} method). (A {@code null} value is permitted,
	 *              and indicates that the cause is nonexistent or unknown.)
	 */
	public AccountingException(final Throwable cause) {
		super(cause);
	}
}

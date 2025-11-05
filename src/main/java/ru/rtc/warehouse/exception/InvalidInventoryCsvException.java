package ru.rtc.warehouse.exception;

public class InvalidInventoryCsvException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidInventoryCsvException(String message) {
		super(message);
	}

	public InvalidInventoryCsvException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidInventoryCsvException(Throwable cause) {
		super(cause);
	}
}

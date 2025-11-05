package ru.rtc.warehouse.exception;

public class InventoryImportException extends RuntimeException {

	public InventoryImportException(String message) {
		super(message);
	}

	public InventoryImportException(String message, Throwable cause) {
		super(message, cause);
	}

	public InventoryImportException(Throwable cause) {
		super(cause);
	}
}
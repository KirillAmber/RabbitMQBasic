package ru.findabair.exception;

/**
 * Временная ошибка — подлежит retry.
 * Например: внешний сервис временно недоступен.
 */
public class TransientException extends RuntimeException {
  public TransientException(String message) {
    super(message);
  }
  public TransientException(String message, Throwable cause) {
    super(message, cause);
  }
}

package ru.findabair.exception;

/**
 * Постоянная ошибка — retry бессмысленен.
 * Например: неверный формат данных, нарушение бизнес-правил.
 * Сообщение будет проглочено и не уйдёт в DLQ.
 */
public class PermanentException extends RuntimeException {
    public PermanentException(String message) {
        super(message);
    }
    public PermanentException(String message, Throwable cause) {
        super(message, cause);
    }
}

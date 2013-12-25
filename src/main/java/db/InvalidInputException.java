package db;

/**
 * Ошибка во входном текстовом файле
 */
public class InvalidInputException extends Exception {
    InvalidInputException() {
        super();
    }

    InvalidInputException(String s) {
        super(s);
    }

}

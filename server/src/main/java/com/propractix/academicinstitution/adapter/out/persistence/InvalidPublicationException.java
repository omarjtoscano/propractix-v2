package com.propractix.academicinstitution.adapter.out.persistence;

final class InvalidPublicationException extends RuntimeException {

    InvalidPublicationException() {
        super("catalog publication is invalid");
    }
}

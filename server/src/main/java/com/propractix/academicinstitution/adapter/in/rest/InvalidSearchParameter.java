package com.propractix.academicinstitution.adapter.in.rest;

final class InvalidSearchParameter extends RuntimeException {
    private final String field;

    InvalidSearchParameter(String field) {
        super("Invalid catalog search parameter");
        this.field = field;
    }

    String field() { return field; }
}

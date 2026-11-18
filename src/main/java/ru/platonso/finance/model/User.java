package ru.platonso.finance.model;

public record User(String login, String salt, String passwordHash) {}

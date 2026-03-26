package com.gymtracker.security;

import java.io.Serializable;

public record AuthUser(Long id, String email, String displayName) implements Serializable {}

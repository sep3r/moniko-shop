package com.monikoshop.dto;

import java.util.List;

/** Stable, compact pagination response for admin tables. Page numbers start at zero. */
public record AdminPage<T>(List<T> items, int page, int size, long totalItems,
                           int totalPages, boolean first, boolean last) {}

package com.blog.controllers;

import org.jetbrains.annotations.NotNull;

public interface ErrorCallback {
    void onError(@NotNull Throwable t, @NotNull Integer id);
}

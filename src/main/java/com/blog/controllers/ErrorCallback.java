package com.blog.controllers;

import org.jetbrains.annotations.NotNull;

/**
 * This interface is a callback function that allows services to notify about errors.
 * <p>
 * Many services run additional threads for different work that can produce exceptions, so this is a way to catch these errors and
 * mark current task as erroneous.
 * <p>
 * The only realization of this callback function is {@link WebControllersConfiguration#errorCallback()}.
 */
public interface ErrorCallback {
    /**
     * This is function should be called when exception occurs.
     *
     * @param t  Exception occurred
     * @param id Currently executing Task ID in which exception occurred
     */
    void onError(@NotNull Throwable t, @NotNull Integer id);
}

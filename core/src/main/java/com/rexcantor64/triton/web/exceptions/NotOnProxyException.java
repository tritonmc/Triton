package com.rexcantor64.triton.web.exceptions;

/**
 * If proxy support is enabled on config, an action that must only
 * be executed from the proxy was executed on the server.
 * @since 4.0.0
 */
public class NotOnProxyException extends Exception {
}

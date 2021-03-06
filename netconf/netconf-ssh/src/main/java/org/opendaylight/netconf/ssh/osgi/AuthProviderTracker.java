/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.ssh.osgi;

import org.opendaylight.netconf.auth.AuthProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AuthProviderTracker implements ServiceTrackerCustomizer<AuthProvider, AuthProvider>, AuthProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AuthProviderTracker.class);

    private final BundleContext bundleContext;

    private final ServiceTracker<AuthProvider, AuthProvider> listenerTracker;
    private volatile AuthProvider authProvider;

    AuthProviderTracker(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        listenerTracker = new ServiceTracker<>(bundleContext, AuthProvider.class, this);
        listenerTracker.open();
    }

    @Override
    public AuthProvider addingService(final ServiceReference<AuthProvider> reference) {
        LOG.trace("Service {} added", reference);
        this.authProvider = bundleContext.getService(reference);
        return authProvider;
    }

    @Override
    public void modifiedService(final ServiceReference<AuthProvider> reference, final AuthProvider service) {
        final AuthProvider authService = bundleContext.getService(reference);
        LOG.trace("Replacing modified service {} in netconf SSH.", reference);
        this.authProvider = authService;
    }

    @Override
    public void removedService(final ServiceReference<AuthProvider> reference, final AuthProvider service) {
        LOG.trace("Removing service {} from netconf SSH. {}", reference,
                " SSH won't authenticate users until AuthProvider service will be started.");
        this.authProvider = null;
    }

    public void stop() {
        listenerTracker.close();
        this.authProvider = null;
        // sshThread should finish normally since sshServer.close stops processing
    }

    @Override
    public boolean authenticated(final String username, final String password) {
        if (authProvider == null) {
            LOG.warn("AuthProvider is missing, failing authentication");
            return false;
        }
        return authProvider.authenticated(username, password);
    }
}

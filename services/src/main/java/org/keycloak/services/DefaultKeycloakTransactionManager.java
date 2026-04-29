/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.services;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import jakarta.transaction.TransactionManager;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.transaction.JtaTransactionManagerLookup;
import org.keycloak.transaction.JtaTransactionWrapper;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakTransactionManager implements KeycloakTransactionManager {

    private static class TransactionState {
        private final List<KeycloakTransaction> prepare = new LinkedList<>();
        private final List<KeycloakTransaction> transactions = new LinkedList<>();
        private final List<KeycloakTransaction> afterCompletion = new LinkedList<>();
        private boolean active;
        private boolean rollback;
        // Used to prevent double committing/rollback if there is an uncaught exception
        protected boolean completed;
    }

    private final KeycloakSession session;
    private JTAPolicy jtaPolicy = JTAPolicy.REQUIRES_NEW;
    private TransactionState transactionState = new TransactionState();

    public DefaultKeycloakTransactionManager(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void enlist(KeycloakTransaction transaction) {
        if (transactionState.active && !transaction.isActive()) {
            transaction.begin();
        }

        transactionState.transactions.add(transaction);
    }

    @Override
    public void enlistAfterCompletion(KeycloakTransaction transaction) {
        if (transactionState.active && !transaction.isActive()) {
            transaction.begin();
        }

        transactionState.afterCompletion.add(transaction);
    }

    @Override
    public void enlistPrepare(KeycloakTransaction transaction) {
        if (transactionState.active && !transaction.isActive()) {
            transaction.begin();
        }

        transactionState.prepare.add(transaction);
    }

    @Override
    public JTAPolicy getJTAPolicy() {
        return jtaPolicy;
    }

    @Override
    public void setJTAPolicy(JTAPolicy policy) {
        jtaPolicy = policy;

    }

    @Override
    public void begin() {
        if (transactionState.active) {
             throw new IllegalStateException("Transaction already active");
        }

        transactionState.completed = false;

        if (jtaPolicy == JTAPolicy.REQUIRES_NEW) {
            JtaTransactionManagerLookup jtaLookup = session.getProvider(JtaTransactionManagerLookup.class);
            if (jtaLookup != null) {
                TransactionManager tm = jtaLookup.getTransactionManager();
                if (tm != null) {
                   enlist(new JtaTransactionWrapper(session, tm));
                }
            }
        }

        for (KeycloakTransaction tx : transactionState.transactions) {
            tx.begin();
        }

        transactionState.active = true;
    }

    @Override
    public void commit() {
        if (transactionState.completed) {
            return;
        } else {
            transactionState.completed = true;
        }

        TracingProvider tracing = session.getProvider(TracingProvider.class);
        tracing.trace(DefaultKeycloakTransactionManager.class, "commit", span -> {
            RuntimeException exception = null;
            for (KeycloakTransaction tx : transactionState.prepare) {
                try {
                    commitWithTracing(tx, tracing);
                } catch (RuntimeException e) {
                    exception = exception == null ? e : exception;
                }
            }
            if (exception != null) {
                rollback(exception);
                return;
            }
            for (KeycloakTransaction tx : transactionState.transactions) {
                try {
                    commitWithTracing(tx, tracing);
                } catch (RuntimeException e) {
                    exception = exception == null ? e : exception;
                }
            }

            // Don't commit "afterCompletion" if commit of some main transaction failed
            if (exception == null) {
                for (KeycloakTransaction tx : transactionState.afterCompletion) {
                    try {
                        commitWithTracing(tx, tracing);
                    } catch (RuntimeException e) {
                        exception = exception == null ? e : exception;
                    }
                }
            } else {
                for (KeycloakTransaction tx : transactionState.afterCompletion) {
                    try {
                        tx.rollback();
                    } catch (RuntimeException e) {
                        ServicesLogger.LOGGER.exceptionDuringRollback(e);
                    }
                }
            }

            transactionState.active = false;
            if (exception != null) {
                throw exception;
            }
        });
    }

    private static void commitWithTracing(KeycloakTransaction tx, TracingProvider tracing) {
        tracing.trace(tx.getClass(), "commit", span -> {
            tx.commit();
        });
    }

    @Override
    public void rollback() {
        if (transactionState.completed) {
            return;
        } else {
            transactionState.completed = true;
        }

        RuntimeException exception = null;
        rollback(exception);
    }

    protected void rollback(RuntimeException exception) {
        TracingProvider tracing = session.getProvider(TracingProvider.class);

        for (KeycloakTransaction tx : transactionState.transactions) {
            try {
                rollbackWithTracing(tx, tracing);
            } catch (RuntimeException e) {
                exception = exception != null ? e : exception;
            }
        }
        for (KeycloakTransaction tx : transactionState.afterCompletion) {
            try {
                rollbackWithTracing(tx, tracing);
            } catch (RuntimeException e) {
                exception = exception != null ? e : exception;
            }
        }
        transactionState.active = false;
        if (exception != null) {
            throw exception;
        }
    }

    private static void rollbackWithTracing(KeycloakTransaction tx, TracingProvider tracing) {
        tracing.trace(tx.getClass(), "rollback", span -> {
            tx.rollback();
        });
    }

    @Override
    public void setRollbackOnly() {
        transactionState.rollback = true;
    }

    @Override
    public boolean getRollbackOnly() {
        if (transactionState.rollback) {
            return true;
        }

        for (KeycloakTransaction tx : transactionState.transactions) {
            if (tx.getRollbackOnly()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isActive() {
        return transactionState.active;
    }

    @Override
    public <T> T doInTransaction(Supplier<T> task) {
        TransactionState savedState = null;
        if (isActive()) {
            savedState = transactionState;
            this.transactionState = new TransactionState();
        }
        begin();
        try {
            return task.get();
        } catch (Throwable t) {
            this.setRollbackOnly();
            throw t;
        } finally {
            try {
                if (this.getRollbackOnly()) {
                    this.rollback();
                } else {
                    this.commit();
                }
            } finally {
                if (savedState != null) {
                    this.transactionState = savedState;
                }
            }
        }
    }
}

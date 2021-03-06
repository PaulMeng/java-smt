/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.java_smt.solvers.smtinterpol;

import static com.google.common.base.Preconditions.checkNotNull;

import de.uni_freiburg.informatik.ultimate.smtinterpol.LogProxy;

import org.sosy_lab.common.log.LogManager;

import java.util.logging.Level;

/**
 * Implementation of {@link LogProxy} that forwards to {@link LogManager}.
 */
@SuppressWarnings("FormatStringAnnotation")
final class LogProxyForwarder implements LogProxy {

  private static final Level LEVEL_FATAL = Level.SEVERE;

  private static final Level LEVEL_ERROR = Level.WARNING;

  // SMTInterpol level "warn" is too noisy for our levels "warning" or "info"
  // for example because of messages "Already inconsistent." when pushing.
  private static final Level LEVEL_WARN = Level.FINE;

  private static final Level LEVEL_INFO = Level.FINER;

  private static final Level LEVEL_DEBUG = Level.FINEST;

  private static final Level LEVEL_TRACE = Level.ALL;

  private final LogManager delegate;

  LogProxyForwarder(LogManager pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  @Override
  public void outOfMemory(String pArg0) {
    throw new OutOfMemoryError(pArg0);
  }

  @Override
  public boolean canChangeDestination() {
    return false;
  }

  @Override
  public void changeDestination(String pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDestination() {
    return "";
  }

  @Override
  public int getLoglevel() {
    if (delegate.wouldBeLogged(LEVEL_TRACE)) {
      return LogProxy.LOGLEVEL_TRACE;
    } else if (delegate.wouldBeLogged(LEVEL_DEBUG)) {
      return LogProxy.LOGLEVEL_DEBUG;
    } else if (delegate.wouldBeLogged(LEVEL_INFO)) {
      return LogProxy.LOGLEVEL_INFO;
    } else if (delegate.wouldBeLogged(LEVEL_WARN)) {
      return LogProxy.LOGLEVEL_WARN;
    } else if (delegate.wouldBeLogged(LEVEL_ERROR)) {
      return LogProxy.LOGLEVEL_ERROR;
    } else if (delegate.wouldBeLogged(LEVEL_FATAL)) {
      return LogProxy.LOGLEVEL_FATAL;
    }
    return LogProxy.LOGLEVEL_OFF;
  }

  @Override
  public void setLoglevel(int pArg0) {
    // ignore
  }

  @Override
  public boolean isFatalEnabled() {
    return delegate.wouldBeLogged(LEVEL_FATAL);
  }

  @Override
  public boolean isErrorEnabled() {
    return delegate.wouldBeLogged(LEVEL_ERROR);
  }

  @Override
  public boolean isWarnEnabled() {
    return delegate.wouldBeLogged(LEVEL_WARN);
  }

  @Override
  public boolean isInfoEnabled() {
    return delegate.wouldBeLogged(LEVEL_INFO);
  }

  @Override
  public boolean isDebugEnabled() {
    return delegate.wouldBeLogged(LEVEL_DEBUG);
  }

  @Override
  public boolean isTraceEnabled() {
    return delegate.wouldBeLogged(LEVEL_TRACE);
  }

  @Override
  public void fatal(Object pArg0) {
    delegate.log(LEVEL_FATAL, pArg0);
  }

  @Override
  public void fatal(String pArg0, Object... pArg1) {
    delegate.logf(LEVEL_FATAL, pArg0, pArg1);
  }

  @Override
  public void error(Object pArg0) {
    delegate.log(LEVEL_ERROR, pArg0);
  }

  @Override
  public void error(String pArg0, Object... pArg1) {
    delegate.logf(LEVEL_ERROR, pArg0, pArg1);
  }

  @Override
  public void warn(Object pArg0) {
    delegate.log(LEVEL_WARN, pArg0);
  }

  @Override
  public void warn(String pArg0, Object... pArg1) {
    delegate.logf(LEVEL_WARN, pArg0, pArg1);
  }

  @Override
  public void info(Object pArg0) {
    delegate.log(LEVEL_INFO, pArg0);
  }

  @Override
  public void info(String pArg0, Object... pArg1) {
    delegate.logf(LEVEL_INFO, pArg0, pArg1);
  }

  @Override
  public void debug(Object pArg0) {
    delegate.log(LEVEL_DEBUG, pArg0);
  }

  @Override
  public void debug(String pArg0, Object... pArg1) {
    delegate.logf(LEVEL_DEBUG, pArg0, pArg1);
  }

  @Override
  public void trace(Object pArg0) {
    delegate.log(LEVEL_TRACE, pArg0);
  }

  @Override
  public void trace(String pArg0, Object... pArg1) {
    delegate.logf(LEVEL_TRACE, pArg0, pArg1);
  }
}

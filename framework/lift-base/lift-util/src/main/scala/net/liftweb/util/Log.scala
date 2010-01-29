/*
 * Copyright 2006-2010 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.liftweb {
package util {

/**
 * Mixin for easy creation of a logger with the name of the class
 */
trait Logging {
  @transient val log = LogBoot.loggerByClass(this.getClass)
}

/**
 * The lift Logger
 */
object Log extends LiftLogger {
  lazy val rootLogger: LiftLogger = LogBoot.loggerByName("lift")

  override def trace(msg: => AnyRef) = rootLogger.trace(msg)
  override def trace(msg: => AnyRef, t: => Throwable) = rootLogger.trace(msg, t)

  override def assertLog(assertion: Boolean, msg: => String) = rootLogger.assertLog(assertion, msg)

  override def isEnabledFor(level: LiftLogLevels.Value) = rootLogger.isEnabledFor(level)
  override def isDebugEnabled = rootLogger.isDebugEnabled
  override def debug(msg: => AnyRef) = rootLogger.debug(msg)
  // override def debugF(msg: => AnyRef) = debug(msg)
  override def debug(msg: => AnyRef, t: => Throwable) = rootLogger.debug(msg, t)

  override def isErrorEnabled = rootLogger.isEnabledFor(LiftLogLevels.Error)
  override def error(msg: => AnyRef) = rootLogger.error(msg)
  // override def errorF(msg: => AnyRef) = error(msg)
  override def error(msg: => AnyRef, t: => Throwable) = rootLogger.error(msg, t)

  override def fatal(msg: AnyRef) = rootLogger.fatal(msg)
  // override def fatalF(msg: AnyRef) = fatal(msg)
  override def fatal(msg: AnyRef, t: Throwable) = rootLogger.fatal(msg, t)

  override def level = rootLogger.level
  override def level_=(level: LiftLogLevels.Value) = rootLogger.level = level
  override def name = rootLogger.name
  // def parent = rootLogger.parent

  override def isInfoEnabled = rootLogger.isInfoEnabled
  override def info(msg: => AnyRef) = rootLogger.info(msg)
  def infoF(msg: => AnyRef) = info(msg)
  override def info(msg: => AnyRef, t: => Throwable) = rootLogger.info(msg, t)


  // def isEnabledFor(level: Priority) = rootLogger.isEnabledFor(level)

  override def isWarnEnabled = rootLogger.isWarnEnabled
  override def warn(msg: => AnyRef) = rootLogger.warn(msg)
  // def warnF(msg: => AnyRef) = warn(msg)
  override def warn(msg: => AnyRef, t: => Throwable) = rootLogger.warn(msg, t)

  def never(msg: => AnyRef) {}
  def neverF(msg: => AnyRef) {}
  def never(msg: => AnyRef, t: => Throwable) {}

  override def isTraceEnabled = rootLogger.isTraceEnabled
}

/**
 * This object provides logging setup utilities.
 *
 * See Log4jLogBoot & Slf4jLogBoot for implemeentations
 *
 */
object LogBoot {
  private[util] lazy val checkConfig: Boolean = loggerSetup()
  private[util] var _loggerByName: String => LiftLogger = _
  private[util] var _loggerByClass: Class[_] => LiftLogger = _

  var defaultProps:String = _

  /**
   * Override this to initialize a different logging system
   *
   * For instance to use Logback instead of log4j
   *
   * <pre>
   * LogBoot.loggerSetup = LogbackLogBoot.setup
   * </pre>
   */
  var loggerSetup: () => Boolean = Log4JLogBoot.setup


  /**
   * Return a LiftLogger with the specified name
   */
  var loggerByName: String => LiftLogger = s => if (LogBoot.checkConfig) _loggerByName(s) else NullLogger

 /**
   * Return a LiftLogger for the specified class
   */
  var loggerByClass: Class[_] => LiftLogger = c => if (LogBoot.checkConfig) _loggerByClass(c) else NullLogger
}

object NullLogger extends LiftLogger {

}

/**
 * The generic LiftLogger interface
 *
 * Note that all logging methods (debug, trace) etc. take by-name
 * parameters, which makes it unnecessary to guard calls with
 *
 * <pre>
 * if (logger.isDebugEnabled) logger.debug("my message")
 * </pre>
 */
trait LiftLogger {
  def isTraceEnabled: Boolean = false
  def trace(msg: => AnyRef): Unit = ()
  def trace(msg: => AnyRef, t: => Throwable): Unit = ()

  def assertLog(assertion: Boolean, msg: => String): Unit = ()

  def isDebugEnabled: Boolean = false
  def debug(msg: => AnyRef): Unit = ()
  def debug(msg: => AnyRef, t: => Throwable): Unit = ()

  def isErrorEnabled: Boolean = false
  def error(msg: => AnyRef): Unit = ()
  def error(msg: => AnyRef, t: => Throwable): Unit = ()

  def fatal(msg: AnyRef): Unit = ()
  def fatal(msg: AnyRef, t: Throwable): Unit = ()

  def level: LiftLogLevels.Value = LiftLogLevels.Off
  def level_=(level: LiftLogLevels.Value): Unit = ()
  def name: String = "Null"
  // def parent = logger.getParent

  def isInfoEnabled: Boolean = false
  def info(msg: => AnyRef): Unit = ()
  def info(msg: => AnyRef, t: => Throwable): Unit = ()

  def isEnabledFor(level: LiftLogLevels.Value): Boolean = false

  def isWarnEnabled: Boolean = false
  def warn(msg: => AnyRef): Unit = ()
  def warn(msg: => AnyRef, t: => Throwable): Unit = ()
}

object LiftLogLevels extends Enumeration {
  val All = Value(1, "All")
  val Trace = Value(3, "Trace")
  val Debug = Value(5, "Debug")
  val Warn = Value(7, "Warn")
  val Error = Value(9, "Error")
  val Fatal = Value(11, "Fatal")
  val Info = Value(13, "Info")
  val Off = Value(15, "Off")
}


}
}

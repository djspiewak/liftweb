/*
 * Copyright 2007-2010 WorldWide Conferencing, LLC
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

import org.slf4j.{Logger, LoggerFactory}
import common._
import Helpers._

/**
 * Object use to configure lift to use slf4j for as internal logging.
 * Note that slf4j back-end should be configured previously to use Slf4jLogBoot.
 * To use it, call Slf4jLogBoot.enable():
 * <pre name="code" class="scala">
 * package bootstrap.liftweb
 *
 * import _root_.net.liftweb.util._
 * ...
 * class Boot {
 *   def boot {
 *     Slf4jLogBoot.enable()
 *     ...
 * </pre>
 * You have to add slf4j (and/or the backend) as dependency of your webapp, and you could exclude log4j.
 * I (DavidB) highly recommand using logback as backend for slf4j.
 * ex, add in your pom.xml:
 * <pre>
 *    &lt;dependency>
 *      &lt;groupId>net.liftweb&lt;/groupId>
 *      &lt;artifactId>lift-webkit&lt;/artifactId>
 *      &lt;version>0.7&lt;/version>
 *      &lt;exclusions>
 *        &lt;exclusion>
 *          &lt;groupId>log4j&lt;/groupId>
 *          &lt;artifactId>log4j&lt;/artifactId>
 *        &lt;/exclusion>
 *      &lt;/exclusions>
 *    &lt;/dependency>
 *    &lt;dependency>
 *      &lt;groupId>ch.qos.logback&lt;/groupId>
 *      &lt;artifactId>logback-classic&lt;/artifactId>
 *      &lt;version>0.9.18&lt;/version>
 *    &lt;/dependency>
 * </pre>
 *
 */
@deprecated object Slf4jLogBoot {
  private def _loggerByClass(clz: Class[_]): LiftLogger = new Slf4jLogger(LoggerFactory.getLogger(clz))
  private def _loggerByName(name: String): LiftLogger = new Slf4jLogger(LoggerFactory.getLogger(name))

  /**
   * enable slf4j as logging system for lift (internal, not for lift based application)
   */
   def enable() {
    LogBoot._loggerByName = _loggerByName
    LogBoot._loggerByClass = _loggerByClass
  }
}


/**
 * This object provides logback setup utilities.
 *
 * To provide your own logback configuration, add a logback.xml
 * file to your classpath.
 *
 * If you want to provide a configuration file for a subset of your application
 * or for a specific environment, Lift expects configuration files to be named
 * in a manner relating to the context in which they are being used. The standard
 * name format is:
 *
 * <pre>
 *   modeName.hostName.userName.filename.extension
 * </pre>
 *
 * with hostName and userName being optional, and modeName being one of
 * 'test', 'staging', 'production', 'pilot', 'profile', or 'default.
 * Thus, if you name your logback config file 'default.logback.xml'
 * it will be picked up correctly.
 */
object LogbackLogBoot {
  def setup() = {
    import ch.qos.logback.classic.LoggerContext;
    import ch.qos.logback.core.util.StatusPrinter;
    import ch.qos.logback.classic.joran.JoranConfigurator;

    def findTheFile: Box[(_root_.java.net.URL, String)] = (first(Props.toTry.flatMap(f => {List(f()+"logback.xml")}))
    (name =>tryo(getClass.getResource(name)).filter(_ ne null).map(s => (s, name))))


    findTheFile match {
      case Full((url, name)) => {
          val lc = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext];
          val configurator = new JoranConfigurator();
          configurator.setContext(lc);
          // the context was probably already configured by default configuration
          // rules
          lc.reset();
          configurator.doConfigure(url);
          StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
      }
      case _ =>
    }

    true
  }
}
  
/**
 * Adapter used internally by lift as Logger
 */
class Slf4jLogger(val logger: Logger) extends LiftLogger {
  override def isTraceEnabled = logger.isTraceEnabled
  override def trace(msg: => AnyRef) = if (isTraceEnabled) logger.trace(String.valueOf(msg))
  override def trace(msg: => AnyRef, t: => Throwable) = if (isTraceEnabled) logger.trace(String.valueOf(msg), t)

  override def assertLog(assertion: Boolean, msg: => String) = if (assertion) info(msg)

  override def isDebugEnabled = logger.isDebugEnabled
  override def debug(msg: => AnyRef) = if (isDebugEnabled) logger.debug(String.valueOf(msg))
  override def debug(msg: => AnyRef, t: => Throwable) = if (isDebugEnabled) logger.debug(String.valueOf(msg), t)

  override def isErrorEnabled = logger.isErrorEnabled
  override def error(msg: => AnyRef) = if (isErrorEnabled) logger.error(String.valueOf(msg))
  override def error(msg: => AnyRef, t: => Throwable) = if (isErrorEnabled) logger.error(String.valueOf(msg), t)

  override def fatal(msg: AnyRef) = error(msg)
  override def fatal(msg: AnyRef, t: Throwable) = error(msg, t)

  override def level = LiftLogLevels.All

  override def isEnabledFor(level: LiftLogLevels.Value): Boolean = level match {
    case LiftLogLevels.All => isTraceEnabled
    case LiftLogLevels.Debug => isDebugEnabled
    case LiftLogLevels.Error => isErrorEnabled
    case LiftLogLevels.Warn => isWarnEnabled
    case LiftLogLevels.Fatal => isErrorEnabled
    case LiftLogLevels.Info => isInfoEnabled
    case LiftLogLevels.Trace => isTraceEnabled
    case LiftLogLevels.Off => !isErrorEnabled
  }

  override def name = logger.getName

  override def isInfoEnabled = logger.isInfoEnabled
  override def info(msg: => AnyRef) = if (isInfoEnabled) logger.info(String.valueOf(msg))
  override def info(msg: => AnyRef, t: => Throwable) = if (isInfoEnabled) logger.info(String.valueOf(msg), t)

  override def isWarnEnabled = logger.isWarnEnabled
  override def warn(msg: => AnyRef) = if (isWarnEnabled) logger.warn(String.valueOf(msg))
  override def warn(msg: => AnyRef, t: => Throwable) = if (isWarnEnabled) logger.warn(String.valueOf(msg), t)
}

}
}

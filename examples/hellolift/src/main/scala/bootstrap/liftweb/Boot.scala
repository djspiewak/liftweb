package bootstrap.liftweb

import _root_.net.liftweb.common._
import _root_.net.liftweb.util._
import _root_.net.liftweb.http._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import Helpers._
import _root_.net.liftweb.mapper._
import _root_.java.sql.{Connection, DriverManager}
import _root_.com.hellolift.model._

/**
  * A class that's instantiated early and run.  It allows the application
  * to modify lift's environment
  */
class Boot {
  def boot {
    // Use Locback over Slf4j for logging - Slf4j and Logback dependencies should be included
    LogBoot.loggerSetup = LogbackLogBoot.setup

    Log.info("Here's a log message logged with logback over Slf4j!")

    // add the connection manager if there's not already a JNDI connection defined
    if (!DB.jndiJdbcConnAvailable_?) DB.defineConnectionManager(DefaultConnectionIdentifier, DBVendor)

    // add the com.hellolift package to the list packages
    // searched for Snippets, CometWidgets, etc.
    LiftRules.addToPackages("com.hellolift")

    // Update the database schema to be in sync
    Schemifier.schemify(true, Log.infoF _, User, Entry)

    // The locale is either calculated based on the incoming user or
    // based on the http request
    LiftRules.localeCalculator = r => User.currentUser.map(_.locale.isAsLocale).openOr(LiftRules.defaultLocaleCalculator(r))

    // Build SiteMap
    val entries = Menu(Loc("Home", List("index"), "Home")) ::
    Menu(Loc("Request Details", List("request"), "Request Details")) ::
    User.sitemap ::: Entry.sitemap

    LiftRules.setSiteMap(SiteMap(entries:_*))

    // Clear the MDC when the request finishes
    S.addAround(Log.clearMDC)

    // Put the id of the logged in user in the MDC
    S.addAround( new LoanWrapper {
      def apply[T](f: => T): T = {
        Log.put("connid" -> "?")
        User.currentUserId.foreach { id => Log.put("userid"->id) }
        f
      }
    })
  }
}

object DBVendor extends ConnectionManager {
  def newConnection(name: ConnectionIdentifier): Box[Connection] = {
    try {
      Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
      val dm = DriverManager.getConnection("jdbc:derby:lift_example;create=true")

      Log.debug("This is logged with default connid")

      // Lame attempt to scope new MDC value
      Log.doWith("connid" -> dm.hashCode) {
        Log.debug("this is logged with new connid")
      }
      Log.debug("Now connid is restored")
      Full(dm)
    } catch {
      case e : Exception => e.printStackTrace; Empty
    }
  }
  def releaseConnection(conn: Connection) {conn.close}
}

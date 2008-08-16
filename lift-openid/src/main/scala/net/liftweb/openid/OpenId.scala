/*
* Copyright 2008 WorldWide Conferencing, LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions
* and limitations under the License.
*/

package net.liftweb.openid

import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message._
import org.openid4java.OpenIDException;
import org.openid4java.consumer._

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.io.IOException;

import net.liftweb._
import http._
import util._

import scala.xml.{NodeSeq, Text}

trait OpenIdVendor {
  type UserType
  
  type ConsumerType <: OpenIDConsumer[UserType]
  
  private object RedirectBackTo extends SessionVar[Can[String]](Empty)
  val PathRoot = "openid"
  
  val LoginPath = "login"
  
  val LogOutPath = "logout"
  
  val ResponsePath = "response"
  
  val PostParamName = "openIdUrl"
  
  val SnippetPrefix = "openId"
  
  def postLogin(id: Can[Identifier],res: VerificationResult): Unit
  
  def postUrl = "/"+ PathRoot + "/" + LoginPath
  
  /**
  * A session var that keeps track of the OpenID object through the request/response
  */
  object OpenIdObject extends SessionVar[ConsumerType](createAConsumer)
  
  def createAConsumer: ConsumerType
  
  def currentUser: Can[UserType]
  
  def snippetPf: LiftRules.SnippetPf = {
    case SnippetPrefix :: "ifLoggedIn" :: Nil => showIfLoggedIn
    case SnippetPrefix :: "ifLoggedOut" :: Nil => showIfLoggedOut
    case SnippetPrefix :: "userBox" :: Nil => showUserBox
  }
  
  def displayUser(id: UserType): NodeSeq
  
  def logoutLink: NodeSeq = <xml:group> <a href={"/"+PathRoot+"/"+LogOutPath}>Log Out</a></xml:group>
  
  def loginForm: NodeSeq = <form method="post" action={"/"+PathRoot+"/"+LoginPath}>
  OpenId <input class="openidfield" name={PostParamName}/> <input type='submit' value="Log In"/>
  </form>
  
  def showUserBox(ignore: NodeSeq): NodeSeq = <div class="openidbox">{
    currentUser match {
      case Full(user) => displayUser(user) ++ logoutLink
      case _ => loginForm
    }
  }</div>
  
  def showIfLoggedIn(in: NodeSeq): NodeSeq = currentUser match {
    case Full(_) => in
    case _ => Text("")
  }
  
  def showIfLoggedOut(in: NodeSeq): NodeSeq = currentUser match {
    case Full(_) => Text("")
    case _ => in
  }
  
  def logUserOut(): Unit
  
/**
 * Try to log a user into the system with a given openId
 */
def loginAndRedirect(openId: String, onComplete: (Can[Identifier], Can[VerificationResult], Can[Exception]) => LiftResponse) {
  val oid = OpenIdObject.is
  oid.onComplete = Full(onComplete)

  throw ResponseShortcutException.shortcutResponse(try {
      oid.authRequest(openId, "/"+PathRoot+"/"+ResponsePath)
    } catch {
      case e: Exception => onComplete(Empty, Empty, Full(e))
    })
}
  
  def dispatchPf: LiftRules.DispatchPf = {
    case RequestMatcher(RequestState(PathRoot :: LogOutPath :: Nil, "", _), _) =>
    req => {
      logUserOut()
      Full(RedirectResponse(S.referer openOr "/", S responseCookies :_*))
    }
    
    case RequestMatcher(r @ RequestState(PathRoot :: LoginPath :: Nil, "", PostRequest), _)
    if r.param(PostParamName).isDefined =>
    req => {
      try {
        RedirectBackTo(S.referer)
        Full(OpenIdObject.is.authRequest(r.param(PostParamName).get, "/"+PathRoot+"/"+ResponsePath))
      } catch {
        case e => S.error("OpenID Failure: "+e.getMessage)
        // FIXME -- log the name and the error
        Full(RedirectResponse(S.referer openOr "/", S responseCookies :_*))
      }
    }
    
    case RequestMatcher(r @ RequestState(PathRoot :: ResponsePath :: Nil, "", _), _) =>
    req => {
      val (id, res) = OpenIdObject.is.verifyResponse(req.request)
      
      OpenIdObject.onComplete match {
        case Full(f) => Full(f(id, Full(res), Empty))
        
        case _ => postLogin(id, res)
        val rb = RedirectBackTo.is
        Full(RedirectResponse(rb openOr "/", S responseCookies :_*))
      }
      
      
    }
  }
}

trait SimpleOpenIdVendor extends OpenIdVendor {
  type UserType = Identifier
  type ConsumerType = OpenIDConsumer[UserType]
  
  def currentUser = OpenIdUser.is
  
  def postLogin(id: Can[Identifier],res: VerificationResult): Unit = {
    id match {
      case Full(id) => S.notice("Welcome "+id)
      
      case _ => S.error("Failed to authenticate")
    }
    
    OpenIdUser(id)
  }
  
  def logUserOut() {
    OpenIdUser.remove
  }
  
  def displayUser(in: UserType): NodeSeq = Text("Welcome "+in)
  
  def createAConsumer = new AnyRef with OpenIDConsumer[UserType]
}

object SimpleOpenIdVendor extends SimpleOpenIdVendor

// object SimpleOpenIdVendor extends SimpleOpenIdVendor

object OpenIdUser extends SessionVar[Can[Identifier]](Empty)

/** * Sample Consumer (Relying Party) implementation.  */
trait OpenIDConsumer[UserType]
{
  val manager = new ConsumerManager
  
  var onComplete: Can[(Can[Identifier], Can[VerificationResult], Can[Exception]) => LiftResponse] = Empty 
  
  // --- placing the authentication request ---
  def authRequest(userSuppliedString: String, targetUrl: String): LiftResponse =
  {
    // configure the return_to URL where your application will receive
    // the authentication responses from the OpenID provider
    val returnToUrl = S.hostAndPath + targetUrl

    Log.info("Creating openId auth request.  returnToUrl: "+returnToUrl)
    
    // perform discovery on the user-supplied identifier
    val discoveries = manager.discover(userSuppliedString)
    
    // attempt to associate with the OpenID provider
    // and retrieve one service endpoint for authentication
    val discovered = manager.associate(discoveries)
    
    S.servletSession.foreach(_.setAttribute("openid-disc", discovered))
    
    // obtain a AuthRequest message to be sent to the OpenID provider
    val authReq = manager.authenticate(discovered, returnToUrl)
    
    // Attribute Exchange example: fetching the 'email' attribute
    val fetch = FetchRequest.createFetchRequest()
    fetch.addAttribute("email",
    // attribute alias
    "http://schema.openid.net/contact/email",   // type URI
    true);                                      // required
    
    // attach the extension to the authentication request
    authReq.addExtension(fetch);
    
    
    if (! discovered.isVersion2() )
    {
      // Option 1: GET HTTP-redirect to the OpenID Provider endpoint
      // The only method supported in OpenID 1.x
      // redirect-URL usually limited ~2048 bytes
      RedirectResponse(authReq.getDestinationUrl(true))
      //                httpResp.sendRedirect(authReq.getDestinationUrl(true));
    }
    else
    {
      // Option 2: HTML FORM Redirection (Allows payloads >2048 bytes)
      val pm =  authReq.getParameterMap()
      val info: Seq[(String, String)] = pm.keySet.toArray.
      map(k => (k.toString, pm.get(k).toString))
      
      XhtmlResponse(
	    <html xmlns="http://www.w3.org/1999/xhtml">
	    <head>
	    <title>OpenID HTML FORM Redirection</title>
	    </head>
	    <body onload="document.forms['openid-form-redirection'].submit();">
	    <form name="openid-form-redirection" action={authReq.getDestinationUrl(false)} method="post" accept-charset="utf-8">
	    {
        info.map{ case(key, value) =>
          <input type="hidden" name={key} value={value}/>
        }
	    }
	    <button type="submit">Continue...</button>
	    </form>
	    </body>
	    </html>
      ,   Empty, Nil, Nil, 200)
    }
  }
  
  // --- processing the authentication response ---
  def verifyResponse(httpReq: HttpServletRequest): (Can[Identifier], VerificationResult) =
  {
    // extract the parameters from the authentication response
    // (which comes in as a HTTP request from the OpenID provider)
    val response =	new ParameterList(httpReq.getParameterMap());
    
    // retrieve the previously stored discovery information
    val discovered = httpReq.getSession().getAttribute("openid-disc") match {
      case d: DiscoveryInformation => d
      case null => throw ResponseShortcutException.redirect("/")
    }
    
    // extract the receiving URL from the HTTP request
    val receivingURL = httpReq.getRequestURL()
    val queryString = httpReq.getQueryString()
    if (queryString != null && queryString.length() > 0)
    receivingURL.append("?").append(httpReq.getQueryString());
    
    // verify the response; ConsumerManager needs to be the same
    // (static) instance used to place the authentication request
    val verification = manager.verify(receivingURL.toString(),
    response, discovered)
    
    // examine the verification result and extract the verified identifier
    
    val verified = verification.getVerifiedId();
    
    (Can.legacyNullTest(verified), verification)
  }
}



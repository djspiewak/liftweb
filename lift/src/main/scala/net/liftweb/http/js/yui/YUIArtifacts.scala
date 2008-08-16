/*
 * Copyright 2007-2008 WorldWide Conferencing, LLC
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

package net.liftweb.http.js.yui

import scala.xml.{Elem, NodeSeq}

import net.liftweb.http.S
import net.liftweb.http.js.JE
import net.liftweb.http.js.JsCmds
import net.liftweb.util.Helpers
import Helpers._
import JsCmds._
import JE._

/**
 * Prerequisite YUI scripts:
 * yahoo.js
 * dom.js
 * connection.js
 * event.js
 */
object YUIArtifacts extends JSArtifacts {
  
  def toggle(id: String) = new JsExp {
    def toJsCmd = "YAHOO.lift.toggle(this, '" + id + "');"; 
  }
  
  def hide(id: String) = new JsExp {
    def toJsCmd = "YAHOO.util.Dom.setStyle('" + id + "', 'display', 'none');" 
  }
  
  def show(id: String) = new JsExp {
    def toJsCmd = "YAHOO.util.Dom.setStyle('" + id + "', 'display', 'block');" 
  }
  
  def showAndFocus(id: String) = new JsExp {
    def toJsCmd = "YAHOO.util.Dom.setStyle('" + id + "', 'display', 'block');" + 
                  "setTimeout(function() { document.getElementById('" + id + "').focus(); }, 200);" 
  }
  
  def serialize(id: String) = new JsExp {
    def toJsCmd = "YAHOO.util.Connect.setForm('" + id+"', false)" 
  }
  
  def setHtml(uid: String, content: NodeSeq): JsCmd = new JsCmd {
    def toJsCmd = "try{document.getElementById('" + uid + "').innerHTML = " + fixHtml(uid, content)+";} catch (e) {}"
  }
  
  def onLoad(cmd: JsCmd): JsCmd = new JsCmd {
    def toJsCmd = "YAHOO.util.Event.onDOMReady(function(){" + cmd.toJsCmd + "})"
  }
  
  def ajax(data: AjaxInfo): String = {
    val url = S.encodeURL(S.contextPath+"/"+LiftRules.ajaxPath);
    "url = YAHOO.lift.buildURI('" + url + "', " + data.data + ");" + 
    "YAHOO.util.Connect.asyncRequest('" + data.action + "', url, " + toJson(data) + ");";
  }
  
  def comet(data: AjaxInfo): String = {
    val url = S.encodeURL(S.contextPath+"/"+LiftRules.cometPath);
    "url = YAHOO.lift.buildURI('" + url + "', YAHOO.lift.simpleJsonToQS(" + data.data + "));" + 
    "YAHOO.util.Connect.asyncRequest('" + data.action + "', url, " + toJson(data) + ");";
  }
  
  def jsonStringify(in: JsExp) : JsExp = new JsExp {
    def toJsCmd = "YAHOO.lang.JSON.stringify(" + in.toJsCmd + ")"
  }
  
  def formToJSON(formId: String):JsExp = new JsExp() {
    def toJsCmd = "YAHOO.lift.formToJSON('" + formId + "')";
  }

  private def toJson(info: AjaxInfo): String = 
    ("timeout : " + info.timeout ::
     "cache : " + info.cache ::  
     "success : function(resp) { res = YAHOO.lift.eval(resp);" +  info.successFunc.map(_ + "(res);").openOr("") + "}" ::
     "failure : " + info.failFunc.openOr ("function (arg) {YAHOO.log('Ajax request failed');}") :: 
     Nil) mkString("{ ", ", ", " }")


}
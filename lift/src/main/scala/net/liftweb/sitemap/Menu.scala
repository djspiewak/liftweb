package net.liftweb.sitemap

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

import net.liftweb.http._
import net.liftweb.util._
import Helpers._

case class Menu(loc: Loc, kids: Menu*) extends HasKids {
  private[sitemap] var _parent: Can[HasKids] = Empty
  private[sitemap] var siteMap: SiteMap = _

  private[sitemap] def init(siteMap: SiteMap) {
    this.siteMap = siteMap
    kids.foreach(_._parent = Full(this))
    kids.foreach(_.init(siteMap))
    loc.setMenu(this)
  }

  private[sitemap] def validate {
    _parent.foreach(p => if (p.isRoot_?) throw new SiteMapException("Menu items with root location (\"/\") cannot have children"))
    kids.foreach(_.validate)
  }

  private[sitemap] def testParentAccess: Either[Boolean, Can[LiftResponse]] = _parent match {
    case Full(p) => p.testAccess
    case _ => Left(true)
  }

  override private[sitemap] def testAccess: Either[Boolean, Can[LiftResponse]] = loc.testAccess 

  def findLoc(req: RequestState): Can[Loc] = 
  if (loc.doesMatch_?(req)) Full(loc)
  else first(kids)(_.findLoc(req))
 
  def buildThisLine(loc: Loc) = {
    val menuList = _parent.map(_.kids) openOr List(this)
    MenuLine(menuList.flatMap{
      mi =>
      val p = mi.loc
      val same = loc eq p
      p.buildItem(same, same)
    })
  }

  def buildChildLine = MenuLine(kids.flatMap(m => m.loc.buildItem(false, false)))
  override def buildUpperLines: Seq[MenuLine] = _parent match {
    case Full(p) => p.buildUpperLines.toList ::: p.buildAboveLine(this).toList
    case _ => Nil
  }

  override def buildAboveLine(path: Menu): Seq[MenuLine] = _parent match {
    case Full(p) => List(MenuLine(p.kids.flatMap(m => m.loc.buildItem(false, m eq path))))
    case _ => Nil
  }
}


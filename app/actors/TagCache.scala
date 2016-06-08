/*   __                          __                                          *\
*   / /____ ___ ____  ___  ___ _/ /       lasius                      *
*  / __/ -_) _ `/ _ \/ _ \/ _ `/ /        contributed by tegonal              *
*  \__/\__/\_, /\___/_//_/\_,_/_/         http://tegonal.com/                 *
*         /___/                                                               *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by    *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package actors

import akka.actor._
import models._
import shapeless._
import core.Global
import scala.reflect.runtime.universe._
import scala.reflect.ClassTag

object TagCache {  
  
  case class GetTags(projectId: ProjectId)
  case class CachedTags(projectId: ProjectId, tags: Set[BaseTag])
  case class TagsUpdated[X <: BaseTag](projectId: ProjectId, tags: Set[X])(implicit m : Manifest[X]) {
     val manifest = m
  }  
  
  def props(): Props = Props(classOf[DefaultTagCache])
}

class DefaultTagCache extends TagCache with DefaultClientReceiverComponent

trait TagCache extends Actor with ActorLogging {
  self: ClientReceiverComponent =>
    
    import TagCache._
    import scala.reflect.runtime.universe._    
    
    var tagCache:Map[ProjectId, Map[Manifest[_], Set[BaseTag]]] = Map()
  
  val receive: Receive = {
    case t @ TagsUpdated(projectId, tags) =>      
      adjustCache(t.manifest, projectId, tags)
    case GetTags(projectId) => {
      val tags = tagCache.get(projectId).map{map => map.map(_._2).toSet.flatten        
      }.getOrElse(Set())
      sender ! CachedTags(projectId, tags)
    }     
  }
        
    
    def adjustCache[T <: BaseTag: TypeTag](typ: Manifest[T], projectId:ProjectId, tags: Set[BaseTag]) = {
      // Needed to allow V to be inferred in Case1 resolution (ie. map sand pairApply)
      implicit object MS extends (Manifest ~?> Set)      
      
      val currentMap = tagCache.get(projectId)
      val current = currentMap.map(_.get(typ).getOrElse(Set())).getOrElse(Set())
      
      val diff = calcDiff(current, tags)
      
      //notify client
      log.debug(s"TagCache changed:$diff")
      tagCache = diff.map{ case (removed, added) => 
        val msg = TagCacheChanged(projectId, removed, added)
        clientReceiver broadcast (Global.systemUser, msg)        
      
        //update cache
        val newMap = currentMap.map(_ + (typ -> tags)).getOrElse(Map())
        tagCache + (projectId -> newMap)
      }.getOrElse(tagCache)
    }
    
    def calcDiff(current:Set[BaseTag], tags: Set[BaseTag]):Option[(Set[BaseTag], Set[BaseTag])] = {
      //find tags removed
      val removed = diff(current, tags) 
      
      //tag tags added
      val added = diff(tags, current)
      
      if ((removed.size + added.size) > 0) 
        Some((removed, added))
      else
        None
    }
    
    def diff(current: Set[BaseTag], updated: Set[BaseTag]) = current --(updated)    
}
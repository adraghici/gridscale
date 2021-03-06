/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.iscpif.gridscale.pbs

import java.util.UUID

import fr.iscpif.gridscale.jobservice._
import fr.iscpif.gridscale.tools._

import scala.concurrent.duration.Duration

trait PBSJobDescription extends JobDescription {
  val uniqId = UUID.randomUUID.toString
  def workDirectory: String
  def queue: Option[String] = None
  def wallTime: Option[Duration] = None
  def memory: Option[Int] = None
  def nodes: Option[Int] = None
  def coreByNode: Option[Int] = None
  def output: String = uniqId + ".out"
  def error: String = uniqId + ".err"

  def toPBS = {
    val buffer = new ScriptBuffer
    buffer += "#!/bin/bash"

    buffer += "#PBS -o " + output
    buffer += "#PBS -e " + error

    queue foreach { q ⇒ buffer += "#PBS -q " + q }
    memory foreach { m ⇒ buffer += "#PBS -lmem=" + m + "mb" }
    wallTime foreach { t ⇒ buffer += "#PBS -lwalltime=" + t.toHHmmss }

    nodes match {
      case Some(n) ⇒ buffer += "#PBS -lnodes=" + n + ":ppn=" + coreByNode.getOrElse(1)
      case None    ⇒ coreByNode foreach { c ⇒ buffer += "#PBS -lnodes=1:ppn=" + c }
    }

    buffer += "cd " + workDirectory

    buffer += executable + " " + arguments
    buffer.toString
  }
}

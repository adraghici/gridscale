/*
 * Copyright (C) 2016 Adrian Draghici
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

package fr.iscpif.gridscale.aws

import fr.iscpif.gridscale.tools.ScriptBuffer

object StarclusterConfig {
  val ClusterTemplateName = "jobcluster"
  val ClusterUser = "sgeadmin"
  val ClusterShell = "bash"
  val ClusterImage = "ami-3393a45a"
}

import fr.iscpif.gridscale.aws.StarclusterConfig._

class StarclusterConfig(
    awsUserId: String,
    awsKeyId: String,
    awsSecretKey: String,
    keypairName: String,
    privateKeyPath: String,
    clusterInstanceType: String,
    clusterSize: Int = 1) {

  override def toString = {
    val buf = new ScriptBuffer
    buf += templateDefinition
    buf += awsCredentials
    buf += privateKey
    buf += clusterSpecs
    buf.toString
  }

  private def templateDefinition = {
    val buf = new ScriptBuffer
    buf += section("global")
    buf += assign("DEFAULT_TEMPLATE", ClusterTemplateName)
    buf.toString
  }

  private def awsCredentials = {
    val buf = new ScriptBuffer
    buf += section("aws info")
    buf += assign("AWS_ACCESS_KEY_ID", awsKeyId)
    buf += assign("AWS_SECRET_ACCESS_KEY", awsSecretKey)
    buf += assign("AWS_USER_ID", awsUserId)
    buf.toString
  }

  private def privateKey = {
    val buf = new ScriptBuffer
    buf += section(s"key $keypairName")
    buf += assign("KEY_LOCATION", privateKeyPath)
    buf.toString
  }

  private def clusterSpecs = {
    val buf = new ScriptBuffer
    buf += section(s"cluster $ClusterTemplateName")
    buf += assign("KEYNAME", keypairName)
    buf += assign("CLUSTER_SIZE", clusterSize.toString)
    buf += assign("CLUSTER_USER", ClusterUser)
    buf += assign("CLUSTER_SHELL", ClusterShell)
    buf += assign("NODE_IMAGE_ID", ClusterImage)
    buf += assign("NODE_INSTANCE_TYPE", clusterInstanceType)
    buf.toString
  }

  private def section(name: String) = {
    "[" + name + "]"
  }

  private def assign(parameter: String, value: String) = {
    parameter + " = " + value
  }
}

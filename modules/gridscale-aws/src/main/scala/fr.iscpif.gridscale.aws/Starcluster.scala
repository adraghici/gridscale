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

import java.io.PrintWriter
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{ Files, Paths }
import java.util.UUID

import fr.iscpif.gridscale.aws.AWSJobService.Gridscale
import fr.iscpif.gridscale.ssh.SSHJobService._
import fr.iscpif.gridscale.tools.ScriptBuffer
import fr.iscpif.gridscale.tools.shell.BashShell
import resource.managed

object Starcluster {
  val UniqId = UUID.randomUUID.toString
  val Name = "gridscale-cluster" + UniqId
  val Tool = "starcluster"
  val Template = "jobcluster"
  val User = "sgeadmin"
  val Shell = "bash"
  val Image = "ami-3393a45a"
  val KeypairName = "starcluster-" + UniqId
  val OwnerReadWritePermissions = "rw-------"
  val SharedHome = "/home"

  def apply(service: AWSJobService, config: Config) = new Starcluster(service, config)

  class Config(
      awsUserId: String,
      awsKeyId: String,
      awsSecretKey: String,
      privateKeyPath: String,
      instanceType: String,
      val size: Int = 1) {

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
      buf += assign("DEFAULT_TEMPLATE", Template)
      buf += assign("ENABLE_EXPERIMENTAL", "True")
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
      buf += section(s"key $KeypairName")
      buf += assign("KEY_LOCATION", s".starcluster/$KeypairName")
      buf.toString
    }

    private def clusterSpecs = {
      val buf = new ScriptBuffer
      buf += section(s"cluster $Template")
      buf += assign("KEYNAME", KeypairName)
      buf += assign("CLUSTER_SIZE", size.toString)
      buf += assign("CLUSTER_USER", User)
      buf += assign("CLUSTER_SHELL", Shell)
      buf += assign("NODE_IMAGE_ID", Image)
      buf += assign("NODE_INSTANCE_TYPE", instanceType)
      buf.toString
    }

    private def section(name: String) = {
      "[" + name + "]"
    }

    private def assign(parameter: String, value: String) = {
      parameter + " = " + value
    }
  }
}

import fr.iscpif.gridscale.aws.Starcluster._

class Starcluster(service: AWSJobService, config: Starcluster.Config) extends BashShell {
  private val userHome: String = System.getProperty("user.home")
  lazy val path = s"${userHome}/${hidden("starcluster")}"
  lazy val privateKeyPath = userHome + s"/${hidden(Gridscale)}/$KeypairName"

  def configure() = {
    service.makeDir(path)
    writeConfig()
    createKeypair()
  }

  def start() = service.withConnection { implicit connection ⇒
    exec(cmd("start", Name))
    loadbalance(config.size)
  }

  def terminate() = service.withConnection { implicit connection ⇒
    exec("echo y | " + cmd("terminate", "-f", Name))
  }

  def addNodes(count: Int) = service.withConnection { implicit connection ⇒
    exec(cmd("addnode", ("-n", count.toString), Name))
  }

  def masterIp = service.withConnection { implicit connection ⇒
    val info = execReturnCodeOutput(cmd("listinstances"))._2
    val masterInfo = info.split("\n\n").filter(_.contains("master")).head
    val pattern = """public_ip:\s([\.\d]+)""".r
    pattern.findAllIn(masterInfo).matchData.next.group(1)
  }

  private def loadbalance(maxNodes: Int) = service.withConnection { implicit connection ⇒
    exec("nohup " + cmd("loadbalance", ("-m", maxNodes.toString), Name) + s" > ${path}/loadbalancer.log 2>&1 &")
  }

  private def writeConfig() = service.withConnection { implicit connection ⇒
    service.write(config.toString.getBytes, s"$path/config")
  }

  private def createKeypair() = service.withConnection { implicit connection ⇒
    exec(cmd("createkey", ("-o", s"$path/$KeypairName"), KeypairName))
    // Hack because reading doesn't work
    val (_, privateKey, _) = execReturnCodeOutput(s"cat $path/$KeypairName")
    val out = Paths.get(s"$privateKeyPath")
    Files.createDirectories(out.getParent)
    Files.createFile(out)
    Files.setPosixFilePermissions(out, PosixFilePermissions.fromString(OwnerReadWritePermissions))
    managed(new PrintWriter(out.toString)) acquireAndGet { _.print(privateKey) }
  }

  private def hidden(dir: String) = "." + dir

  private def cmd(instruction: String) = {
    s"$Tool $instruction"
  }

  private def cmd(instruction: String, arg: String) = {
    s"$Tool $instruction $arg"
  }

  private def cmd(instruction: String, option: String, arg: String): String = {
    cmd(instruction, (option, ""), arg)
  }

  private def cmd(instruction: String, option: (String, String), arg: String) = {
    List(Tool, instruction, option._1, option._2, arg).mkString(" ")
  }
}

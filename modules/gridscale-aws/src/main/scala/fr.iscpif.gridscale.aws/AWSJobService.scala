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

import java.io.File

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.io.Source

import fr.iscpif.gridscale.authentication.PrivateKey
import fr.iscpif.gridscale.aws.AWSJobService._
import fr.iscpif.gridscale.jobservice._
import fr.iscpif.gridscale.sge.{ SGEJobDescription, SGEJobService }
import fr.iscpif.gridscale.ssh._
import fr.iscpif.gridscale.tools.shell.BashShell
import org.jclouds.ContextBuilder
import org.jclouds.aws.ec2.compute.AWSEC2TemplateOptions
import org.jclouds.compute.{ ComputeService, ComputeServiceContext }
import org.jclouds.compute.domain.{ NodeMetadata, OsFamily, Template }
import org.jclouds.ec2.domain.InstanceType
import org.jclouds.sshj.config.SshjSshClientModule

object AWSJobService {

  val Gridscale = "gridscale"
  val Provider = "aws-ec2"
  val CoordinatorImageId = "ami-b7d8cedd"
  val Group = "gridscale-aws"
  val User = "ubuntu"
  val Root = "root"
  val DefaultInstanceType = "m1.small"
  val ClusterName = "gridscale-cluster"

  def apply(config: Config) = {
    val (keyId, secretKey) = readAWSCredentials(config.awsUserName, config.awsCredentialsPath)
    val starclusterConfig = new Starcluster.Config(
      awsKeyId = keyId,
      awsSecretKey = secretKey,
      awsUserId = config.awsUserId,
      privateKeyPath = config.privateKeyPath,
      instanceType = DefaultInstanceType,
      size = config.clusterSize)

    new AWSJobService {
      override val credential = createCredential(config.privateKeyPath)
      override lazy val host = createHost(coordinator)
      override val port = 22
      override val timeout = 1 minute
      override val region = config.region
      override val awsKeypairName = config.awsKeypairName
      override val client = createClient(keyId, secretKey)
      override val starcluster: Starcluster = Starcluster(this, starclusterConfig)
      override lazy val sge = createSGEJobService(starcluster)
    }
  }

  def createClient(awsKeyId: String, awsSecretKey: String): ComputeService = {
    ContextBuilder.newBuilder(Provider)
      .credentials(awsKeyId, awsSecretKey)
      .modules(Set(new SshjSshClientModule).asJava)
      .buildView(classOf[ComputeServiceContext])
      .getComputeService
  }

  def createCredential(privateKeyPath: String) = {
    sshPrivateKey(PrivateKey(User, new File(privateKeyPath), ""))
  }

  def createHost(coordinator: NodeMetadata) = {
    coordinator.getPublicAddresses.head
  }

  def createSGEJobService(starcluster: Starcluster) = {
    SGEJobService(starcluster.masterIp)(PrivateKey(Root, new File(starcluster.privateKeyPath), ""))
  }

  def readAWSCredentials(user: String, path: String): (String, String) = {
    resource.managed(Source.fromFile(path)) acquireAndGet {
      src ⇒
        {
          val Array(_, keyId, secretKey) = src.getLines.drop(1).map(_.split(",")).filter(line ⇒ user == extractUser(line)).next
          (keyId, secretKey)
        }
    }
  }

  def createImageId(region: String, ami: String): String = region + "/" + ami

  def extractUser(credentialLine: Array[String]) = credentialLine(0).stripPrefix("\"").stripSuffix("\"")

  class Config(
      val region: String,
      val awsUserName: String,
      val awsUserId: String,
      val awsKeypairName: String,
      val awsCredentialsPath: String,
      val privateKeyPath: String,
      val clusterSize: Int) {
  }
}

trait AWSJobService extends JobService with SSHHost with SSHStorage with BashShell with AutoCloseable {
  type J = SGEJobService.SGEJob
  type D = SGEJobDescription

  def region: String
  def awsKeypairName: String
  def client: ComputeService
  def starcluster: Starcluster
  def sge: SGEJobService
  var coordinator: NodeMetadata = _

  def submit(description: D): J = sge.submit(description)

  def cancel(job: J) = sge.cancel(job)

  def state(job: J): JobState = sge.state(job)

  def purge(job: J) = sge.purge(job)

  def start() = {
    println("starting coordinator...")
    coordinator = client.createNodesInGroup(Group, 1, createTemplate(region, client)).head
    // Wait for a short period to make sure that{ the VM is initialized and ports are opened
    println("waiting for coordinator initialization...")
    SECONDS.sleep(30)
    println("starcluster setup...")
    starcluster.configure()
    println("starting starcluster...")
    starcluster.start()
    println("starcluster master ip: " + starcluster.masterIp)
    println(s"starcluster private key path: ${starcluster.privateKeyPath}")
    println("setting starcluster master sge environment vars...")
    sge.setEnv(List(
      ("SGE_ROOT", "/opt/sge6"),
      ("PATH", "$PATH:/opt/sge6/bin/linux-x64"),
      ("SGE_QMASTER_PORT", "63231"),
      ("SGE_CLUSTER_NAME", "starcluster"),
      ("SGE_CELL", "default"),
      ("SGE_EXECD_PORT", "63232")))
    println("ready.")
  }

  def addNodes(count: Int) = starcluster.addNodes(count)

  def kill() = {
    println("shutting down starcluster...")
    starcluster.terminate()
    println(s"shutting down coordinator ${coordinator.getId}...")
    client.destroyNode(coordinator.getId)
    println("cleanup done.")
  }

  def close(): Unit = client.getContext.close()

  def sharedHome = Starcluster.SharedHome

  private def createTemplate(region: String, client: ComputeService): Template = {
    val template = client.templateBuilder()
      .hardwareId(InstanceType.T1_MICRO)
      .osFamily(OsFamily.UBUNTU)
      .imageId(createImageId(region, CoordinatorImageId))
      .build()

    val options = template.getOptions.as(classOf[AWSEC2TemplateOptions]).keyPair(awsKeypairName)
    template
  }
}

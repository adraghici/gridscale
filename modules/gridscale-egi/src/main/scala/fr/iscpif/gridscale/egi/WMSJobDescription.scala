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

package fr.iscpif.gridscale.egi

import java.io.File

import fr.iscpif.gridscale.jobservice.JobDescription
import fr.iscpif.gridscale.tools.ScriptBuffer

import scala.concurrent.duration.Duration

trait WMSJobDescription extends JobDescription {
  def inputSandbox: Iterable[File]
  def outputSandbox: Iterable[(String, File)]

  def rank = "(-other.GlueCEStateEstimatedResponseTime)"
  def fuzzy: Boolean = false
  def stdOutput = ""
  def stdError = ""
  def memory: Option[Int] = None
  def cpuTime: Option[Duration] = None
  def cpuNumber: Option[Int] = None
  def wallTime: Option[Duration] = None
  def jobType: Option[String] = None
  def smpGranularity: Option[Int] = None
  def retryCount: Option[Int] = None
  def shallowRetryCount: Option[Int] = None
  def myProxyServer: Option[String] = None
  def architecture: Option[String] = None
  def ce: Option[Iterable[String]] = None

  def requirements =
    "other.GlueCEStateStatus == \"Production\"" +
      memory.map(" && other.GlueHostMainMemoryRAMSize >= " + _).mkString +
      cpuTime.map(" && other.GlueCEPolicyMaxCPUTime >= " + _.toMinutes).mkString +
      wallTime.map(" && other.GlueCEPolicyMaxWallClockTime >= " + _.toMinutes).mkString +
      architecture.map(" && other.GlueHostArchitecturePlatformType == \"" + _ + "\"").mkString +
      ce.map(" && (" + _.map("other.GlueCEUniqueID == \"" + _ + "\"").mkString("|") + ")").mkString

  def toJDL = {
    val script = new ScriptBuffer

    jobType.foreach(script += "JobType = \"" + _ + "\";")
    script += "Executable = \"" + executable + "\";"
    script += "Arguments = \"" + arguments + "\";"

    if (!inputSandbox.isEmpty) script += "InputSandbox = " + sandboxTxt(inputSandbox.map(_.getPath)) + ";"
    if (!outputSandbox.isEmpty) script += "OutputSandbox = " + sandboxTxt(outputSandbox.unzip._1) + ";"

    if (!stdOutput.isEmpty) script += "StdOutput = \"" + stdOutput + "\";"
    if (!stdError.isEmpty) script += "StdError = \"" + stdError + "\";"

    cpuNumber.foreach(script += "CpuNumber = " + _ + ";")
    smpGranularity.foreach(script += "SMPGranularity = " + _ + ";")

    script += "Requirements = " + requirements + ";"
    script += "Rank = " + rank + ";"

    if (fuzzy) script += "FuzzyRank = true;"

    retryCount.foreach(script += "RetryCount = " + _ + ";")
    shallowRetryCount.foreach(script += "ShallowRetryCount = " + _ + ";")

    myProxyServer.foreach(script += "MyProxyServer = \"" + _ + "\";")

    script.toString
  }

  private def sandboxTxt(sandbox: Iterable[String]) =
    "{" + sandbox.map { "\"" + _ + "\"" }.mkString(",") + "}"

}

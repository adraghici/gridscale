/**
 * Copyright (C) 2015 Jonathan Passerat-Palmbach
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package fr.iscpif.gridscale.benchmark
package util

import fr.iscpif.gridscale.aws.{AWSJobDescription, AWSJobService}
import fr.iscpif.gridscale.jobservice.JobService
import fr.iscpif.gridscale.sge.{SGEJobDescription, SGEJobService}

trait BenchmarkUtils {

  def withTimer[T](f: ⇒ T): Option[(T, Double)] = {
    try {
      val before = System.nanoTime()
      val res = f
      val after = System.nanoTime()
      val elapsed = after - before
      Some((res, elapsed / 1000000d))
    } catch {
      case _: Throwable ⇒ None
    }
  }
}

trait BenchmarkConfig {

  def executable = "/bin/sleep"
  def arguments = "1000"
  def workDirectory = "/work/jpassera/benchmark"
}

trait Benchmark extends BenchmarkUtils {

  implicit val jobService: JobService

  type JobDescription = jobService.D

  val jobDescription: JobDescription
  val nbJobs: Int
  val missingValue = -1.0

  def benchmarkSSH(jd: JobDescription)(nbJobs: Int) = {

    val sshJobService = jobService.asInstanceOf[SGEJobService]
    val sshJobDescriptions = jobDescription.asInstanceOf[SGEJobDescription]
    val sshSubmit = sshJobService.submitAsync(_)
    val sshState = sshJobService.stateAsync(_)
    val sshCancel = sshJobService.cancelAsync(_)

    println("Submitting jobs...")
    val (submitRes, submitTime) = sshJobService.withConnection { implicit connection ⇒
      implicit val ssh = (connection, connection.newSFTPClient)
      withTimer {
        sshJobService.jobActions(sshSubmit)(sshJobService.processSubmit)({ for (i ← 1 to nbJobs) yield sshJobDescriptions }: _*) run (connection) // ((connection, sftpClient))
      }
    }.getOrElse(Seq.empty, missingValue)

    val jobs = submitRes.map(_._2)

    println(s"Submitted $nbJobs jobs in $submitTime")

    println("Querying state for jobs...")

    val (states, queryTime) = sshJobService.withConnection(implicit connection ⇒
      withTimer { sshJobService.jobActions(sshState)(sshJobService.processState)(jobs: _*) run (connection) }
    ).getOrElse(Seq.empty, missingValue)

    println(s"Queried state for ${states.length} jobs in $queryTime")

    println("Cancelling jobs...")

    val (_, cancelTime) = sshJobService.withConnection(implicit connection ⇒
      withTimer { sshJobService.jobActions(sshCancel)(sshJobService.processCancel)(jobs: _*) run (connection) }
    ).getOrElse(Seq.empty, missingValue)

    println(s"Cancelled $nbJobs jobs in $cancelTime")

    println("Purging jobs...")
    // only purging one job, they're all the same
    try sshJobService.purge(jobs.head)
    catch { case _: Throwable ⇒ }

    println("Done")

    List(submitTime, queryTime, cancelTime)
  }

  def benchmarkOthers(jd: JobDescription)(nbJobs: Int) = {

    println("Submitting jobs...")
    val (jobs, submitTime) = withTimer {
      (1 to nbJobs).map(x ⇒ jobService.submit(jobDescription))
    }.getOrElse(Seq.empty, missingValue)

    println(s"Submitted $nbJobs jobs in $submitTime")

    println("Querying state for jobs...")
    val (states, queryTime) = withTimer {
      jobs.map(jobService.state)
    }.getOrElse(Seq.empty, missingValue)

    println(s"Queried state for ${states.length} jobs in $queryTime")

    println("Cancelling jobs...")
    val (_, cancelTime) = withTimer(jobs.foreach(jobService.cancel)).getOrElse(Seq.empty, missingValue)
    println(s"Cancelled $nbJobs jobs in $cancelTime")

    println("Purging jobs...")
    // only purging one job, they're all the same
    try jobService.purge(jobs.head)
    catch { case _: Throwable ⇒ }

    println("Done")

    List(submitTime, queryTime, cancelTime)
  }

  def runBenchmark(runs: Int = 1) = for (run ← 1 to runs) yield benchmarkSSH(jobDescription)(nbJobs)

  def runBenchmarkJobs(n: Int) = benchmarkSSH(jobDescription)(n)

  def avgBenchmark(runs: Int) = {
    val res = runBenchmark(runs)
    res.transpose.map(l ⇒ l.filter(_ != missingValue).foldLeft(0.0)(_ + _)).map(_ / runs)
  }

}

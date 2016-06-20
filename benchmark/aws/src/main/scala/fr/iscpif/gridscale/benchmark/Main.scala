/*
 * Copyright (C) 2016 Adrian Draghici
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

import fr.iscpif.gridscale.aws.AWSJobService
import fr.iscpif.gridscale.benchmark.aws.AWSBenchmark

object Main {
  def main(argv: Array[String]): Unit = {

    val (config, nbJobs, nbRuns) = argv match {
      case Array(r, u, id, kp, cP, pkP, s, nbJ, nbR) ⇒ (new AWSJobService.Config(r, u, id, kp, cP, pkP, AWSJobService.MasterInstanceType, AWSJobService.NodeInstanceType, None, s.toInt), nbJ.toInt, nbR.toInt)
      case Array(r, u, id, kp, cP, pkP, s, nbJ, nbR, masterInstanceType, spotBid) ⇒ (new AWSJobService.Config(r, u, id, kp, cP, pkP, masterInstanceType, AWSJobService.NodeInstanceType, Some(spotBid.toDouble), s.toInt), nbJ.toInt, nbR.toInt)
      case _ ⇒ throw new RuntimeException("Bad arguments")
    }

    val b = AWSBenchmark(config)(nbJobs)
    b.runBenchmarkJobs(100)
    b.runBenchmarkJobs(300)
    b.runBenchmarkJobs(500)
    b.runBenchmarkJobs(1000)
//    val (avgSubmit, avgQuery, avgCancel) = b.avgBenchmark(nbRuns).toList match {
//      case List(a, b, c) ⇒ (a, b, c)
//    }

    b.close()

//    println(
//      s"""Average for $nbJobs jobs along $nbRuns runs (milliseconds):
//         |\tsubmit: $avgSubmit
//         |\tstate: $avgQuery
//         |\tcancel: $avgCancel
//       """.stripMargin)
  }
}

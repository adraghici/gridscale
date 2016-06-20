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

package fr.iscpif.gridscale.benchmark
package aws

import fr.iscpif.gridscale.aws._
import fr.iscpif.gridscale.benchmark.util._

object AWSBenchmark {

  def apply(config: AWSJobService.Config)(inNbJobs: Int) = {

    val benchmark = new Benchmark with AutoCloseable {
      implicit val jobService: AWSJobService = AWSJobService(config)

      override val nbJobs = inNbJobs
      override val jobDescription = new AWSJobDescription with BenchmarkConfig {
        override val workDirectory = jobService.home
      }

      override def close() = jobService.close()
    }

    benchmark.jobService.start()

    benchmark
  }
}

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

import java.io.File

import fr.iscpif.gridscale.authentication.PrivateKey
import fr.iscpif.gridscale.benchmark.util._
import fr.iscpif.gridscale.sge.{SGEJobDescription, SGEJobService}

object SGEBenchmark {

  def apply(inHost: String, inUsername: String, inPassword: String, inPrivateKeyPath: String)(inNbJobs: Int) = {

    new Benchmark {
      def credential = PrivateKey(inUsername, new File(inPrivateKeyPath), inPassword)

      implicit val jobService: SGEJobService = SGEJobService(inHost)(credential)

      override val nbJobs = inNbJobs
      override val jobDescription = new SGEJobDescription with BenchmarkConfig
    }
  }
}

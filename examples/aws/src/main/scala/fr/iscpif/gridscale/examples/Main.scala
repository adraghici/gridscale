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

package fr.iscpif.gridscale.examples

import fr.iscpif.gridscale._
import fr.iscpif.gridscale.aws.{ AWSJobDescription, AWSJobService }
import resource.managed

object Main extends App {

  val awsService = AWSJobService(new AWSJobService.Config(
    region = "us-east-1",
    awsUserName = "adrian",
    awsUserId = "788108661243",
    awsKeypairName = "gridscale",
    awsCredentialsPath = "/Users/adrian/.aws/credentials.csv",
    privateKeyPath = "/Users/adrian/.ssh/id_rsa",
    clusterSize = 1))

  managed(awsService) acquireAndGet {
    aws ⇒
      {
        aws.start()
        println("job submission...")
        val description = new AWSJobDescription {
          def executable = "/bin/echo"
          def arguments = "hello > test.txt"
          def workDirectory = aws.sharedHome + "/testjob/"
        }

        val job = aws.submit(description)

        val state = aws.untilFinished(job) { println }

        aws.purge(job)

        aws.kill()
      }
  }
}

/*
 * Copyright (C) 2015 Romain Reuillon
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
package fr.iscpif.gridscale.example.egi.cream

import fr.iscpif.gridscale.egi.BDII
import concurrent.duration._

object CREAMExample extends App {

  val bdii = new BDII("ldap://topbdii.grif.fr:2170")
  val creams = bdii.querySRMLocations("vo.complex-systems.eu", 2 minutes)

  creams.foreach(println)

}

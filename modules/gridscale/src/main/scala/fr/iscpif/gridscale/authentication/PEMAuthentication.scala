/*
 * Copyright (C) 04/06/13 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.iscpif.gridscale.authentication

import java.io.File

object PEMAuthentication {

  def apply(certificate: File, key: File, password: String) = {
    val (_certificate, _key, _password) = (certificate, key, password)

    new PEMAuthentication {
      override val key: File = _key
      override val certificate: File = _certificate
      override val password: String = _password
    }
  }

}

trait PEMAuthentication {
  def certificate: File
  def key: File
  def password: String
}

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

import java.io.{ File, FileInputStream }
import java.util.UUID

import fr.iscpif.gridscale.authentication.AuthenticationException
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl
import org.gridforum.jgss.{ ExtendedGSSCredential, ExtendedGSSManager }
import org.ietf.jgss.GSSCredential

//object ProxyFileAuthentication {
//  def apply(proxy: File) = {
//    val _proxy = proxy
//    new ProxyFileAuthentication {
//      override val proxy: File = _proxy
//    }
//  }
//}
//
//trait ProxyFileAuthentication {
//
//  def proxy: File
//  @transient lazy val delegationID = UUID.randomUUID
//
//  def apply() =
//    try {
//      val proxyBytes = Array.ofDim[Byte](proxy.length.toInt)
//      val in = new FileInputStream(proxy)
//      try in.read(proxyBytes)
//      finally in.close
//
//      val credential = ExtendedGSSManager.getInstance.asInstanceOf[ExtendedGSSManager].createCredential(
//        proxyBytes,
//        ExtendedGSSCredential.IMPEXP_OPAQUE,
//        GSSCredential.DEFAULT_LIFETIME,
//        null, // use default mechanism: GSI
//        GSSCredential.INITIATE_AND_ACCEPT).asInstanceOf[GlobusGSSCredentialImpl]
//
//      GlobusAuthentication.Proxy(credential, proxyBytes, delegationID.toString)
//    } catch {
//      case e: Throwable ⇒ throw AuthenticationException("Error during proxy file authentication", e)
//
//    }
//
//}


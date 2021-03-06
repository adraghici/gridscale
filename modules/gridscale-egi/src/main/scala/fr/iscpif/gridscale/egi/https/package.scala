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
package fr.iscpif.gridscale.egi

import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl._

import org.apache.http.conn.ssl.SSLConnectionSocketFactory

import scala.concurrent.duration.Duration

package object https {

  def trustManager =
    Array[TrustManager](
      new X509TrustManager {
        def getAcceptedIssuers = Array.empty[X509Certificate]
        def checkClientTrusted(certs: Array[X509Certificate], authType: String) {}
        def checkServerTrusted(certs: Array[X509Certificate], authType: String) {}
      })

  def hostVerifier = new HostnameVerifier {
    def verify(hostname: String, session: SSLSession) = true
  }

  def socketFactory(sslContext: SSLContext)(timeout: Duration): SSLConnectionSocketFactory =
    new SSLConnectionSocketFactory(sslContext, hostVerifier) {
      override protected def prepareSocket(socket: SSLSocket) = {
        socket.setSoTimeout(timeout.toMillis.toInt)
      }
    }

}

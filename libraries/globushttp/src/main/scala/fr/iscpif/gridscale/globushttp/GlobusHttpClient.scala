/*
 * Copyright (C) 2014 Romain Reuillon
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

package fr.iscpif.gridscale.globushttp

import java.util.concurrent.Executors

import org.apache.commons.httpclient.methods.{ GetMethod, PostMethod, StringRequestEntity }
import org.apache.commons.httpclient.params.{ HttpClientParams, HttpConnectionManagerParams, HttpMethodParams }
import org.apache.commons.httpclient.protocol.Protocol
import org.apache.commons.httpclient.{ HttpClient, MultiThreadedHttpConnectionManager }
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl
import org.gridforum.jgss.{ ExtendedGSSCredential, ExtendedGSSManager }
import org.ietf.jgss.GSSCredential

object GlobusHttpClient {

  def credential(proxy: Array[Byte]) =
    ExtendedGSSManager.getInstance.asInstanceOf[ExtendedGSSManager].createCredential(
      proxy,
      ExtendedGSSCredential.IMPEXP_OPAQUE,
      GSSCredential.DEFAULT_LIFETIME,
      null, // use default mechanism: GSI
      GSSCredential.INITIATE_AND_ACCEPT).asInstanceOf[GlobusGSSCredentialImpl]

}

trait GlobusHttpClient <: SocketFactory {
  def proxyBytes: Array[Byte]
  def address: java.net.URI
  def defaultProtocolPort = 8446

  def maxConnections: Int

  @transient lazy val manager = {
    val m = new MultiThreadedHttpConnectionManager()
    val param = new HttpConnectionManagerParams
    param.setDefaultMaxConnectionsPerHost(maxConnections)
    param.setMaxTotalConnections(maxConnections)
    param.setSoTimeout(timeout.toMillis.toInt)
    param.setConnectionTimeout(timeout.toMillis.toInt)
    m.setParams(param)
    m
  }

  @transient lazy val httpclient = {
    val myHttpg = new Protocol("https", factory, defaultProtocolPort)
    val httpclient = new HttpClient(manager)
    val param = new HttpClientParams()
    param.setSoTimeout(timeout.toMillis.toInt)
    httpclient.setParams(param)
    httpclient.getHostConfiguration.setHost(address.getHost, address.getPort, myHttpg)
    httpclient
  }

  def post(in: String, address: java.net.URI, headers: Map[String, String]): String = {
    val entity = new StringRequestEntity(in, "text/xml", null)
    val post = new PostMethod(address.getPath)
    try {
      val params = new HttpMethodParams()
      params.setSoTimeout(timeout.toMillis.toInt)
      post.setParams(params)
      post.setRequestEntity(entity)
      headers.foreach { case (k, v) ⇒ post.setRequestHeader(k, v) }
      httpclient.executeMethod(post)
      post.getResponseBodyAsString
    } finally {
      post.releaseConnection
    }
  }

  def get(address: java.net.URI, headers: Map[String, String]): String = {
    val get = new GetMethod(address.getPath)
    try {
      val params = new HttpMethodParams()
      params.setSoTimeout(timeout.toMillis.toInt)
      get.setParams(params)
      headers.foreach { case (k, v) ⇒ get.setRequestHeader(k, v) }
      httpclient.executeMethod(get)
      get.getResponseBodyAsString
    } finally {
      get.releaseConnection
    }
  }

  override def finalize =
    Executors.newSingleThreadExecutor().submit(new Runnable {
      override def run(): Unit = manager.shutdown()
    })

}

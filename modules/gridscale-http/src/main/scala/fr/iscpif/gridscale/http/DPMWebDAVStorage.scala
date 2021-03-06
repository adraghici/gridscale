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
package fr.iscpif.gridscale.http

import java.io._
import java.lang.Thread.UncaughtExceptionHandler
import java.net.URI
import java.util.concurrent.{ TimeUnit, _ }
import java.util.concurrent.atomic.{AtomicLong, AtomicBoolean}

import com.github.sardine.DavResource
import com.github.sardine.impl.SardineRedirectStrategy
import com.github.sardine.impl.handler.MultiStatusResponseHandler
import com.github.sardine.impl.methods.{ HttpMkCol, HttpMove, HttpPropFind }
import fr.iscpif.gridscale.storage._
import org.apache.http._
import org.apache.http.client.methods._
import org.apache.http.entity.InputStreamEntity
import org.apache.http.protocol.{ HttpContext, HTTP }

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

case class WebDAVLocation(host: String, basePath: String, port: Int = 443)

object DPMWebDAVStorage {
  def apply[A: HTTPSAuthentication](location: WebDAVLocation, timeout: Duration = 1 minute)(authentication: A) = {
    val (_location, _timeout) = (location, timeout)
    new DPMWebDAVStorage {
      override def location = _location
      override def factory = implicitly[HTTPSAuthentication[A]].factory(authentication)
      override def timeout = _timeout
    }
  }
}

trait DPMWebDAVStorage <: HTTPSClient with Storage { dav ⇒

  def location: WebDAVLocation
  def timeout: Duration

  def fullUrl(path: String) =
    "https://" + trimSlashes(location.host) + ":" + location.port + "/" + trimSlashes(location.basePath) + "/" + trimSlashes(path)

  override def _write(is: InputStream, path: String) = withClient { httpClient =>
    val countIS = new InputStream {
      var size = 0
      override def read(): Int = {
        val ret = is.read()
        if(ret != -1) size += 1
        ret
      }
    }

    val put = new HttpPut(fullUrl(path))
    val entity = new InputStreamEntity(countIS, -1)
    put.setEntity(entity)
    put.addHeader(HTTP.EXPECT_DIRECTIVE, HTTP.EXPECT_CONTINUE)
    val returnCode = HTTPStorage.execute(httpClient.execute, put)
    val writtenSize = listProp(path).head.getContentLength
    if(writtenSize != countIS.size.toLong) throw new IOException(s"Size of the written file is $writtenSize and does'nt match ${countIS.size} (return code of the request was $returnCode)")
  }

  override def _read(path: String): InputStream = HTTPStorage.toInputStream(new URI(fullUrl(path)), newClient)

  override def _makeDir(path: String): Unit = withClient { httpClient =>
    val mkcol = new HttpMkCol(fullUrl(path))
    HTTPStorage.execute(httpClient.execute, mkcol)
  }

  override def _mv(from: String, to: String): Unit = withClient { httpClient =>
    val move = new HttpMove(fullUrl(from), fullUrl(to), true)
    HTTPStorage.execute(httpClient.execute, move)
  }

  override def _rmDir(path: String): Unit =  withClient { httpClient =>
    val delete = new HttpDelete(fullUrl(path))
    delete.addHeader("Depth", "infinity")
    HTTPStorage.execute(httpClient.execute, delete)
  }

  def listProp(path: String)=   withClient { httpClient =>
      val entity = new HttpPropFind(fullUrl(path))
      entity.setDepth(1.toString)
      try {
        val multistatus = httpClient.execute(entity, new MultiStatusResponseHandler)
        val responses = multistatus.getResponse
        responses.map(new DavResource(_))
      } finally entity.releaseConnection
   }

  override def _list(path: String): Seq[ListEntry] = {
    for { r ← listProp(path).drop(1) } yield {
      ListEntry(
        name = r.getName,
        `type` = if (r.isDirectory) DirectoryType else FileType,
        Some(r.getModified.getTime)
      )
    }
  }

  override def _rmFile(path: String): Unit =  withClient { httpClient =>
    val delete = new HttpDelete(fullUrl(path))
    HTTPStorage.execute(httpClient.execute, delete)
  }

  override def _exists(path: String): Boolean =  withClient { httpClient =>
    val head = new HttpHead(fullUrl(path))
    try {
      val response = httpClient.execute(head)

      try {
        response.getStatusLine.getStatusCode match {
          case x if x < HttpStatus.SC_MULTIPLE_CHOICES ⇒ true
          case HttpStatus.SC_NOT_FOUND ⇒ false
          case _ ⇒ throw new IOException(s"Server responded with an unexpected response: ${response.getStatusLine.getStatusCode} ${response.getStatusLine.getReasonPhrase}")
        }
      } finally response.close()
    } finally head.releaseConnection()
  }

  override def toString = fullUrl("")

}

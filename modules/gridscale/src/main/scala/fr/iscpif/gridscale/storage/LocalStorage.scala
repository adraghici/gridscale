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

package fr.iscpif.gridscale.storage

import java.io._
import java.nio.file._

object LocalStorage {
  def apply() = new LocalStorage {}
}

trait LocalStorage extends Storage {
  override def child(parent: String, child: String) =
    new File(parent, child).getPath

  override def exists(path: String) =
    new File(path).exists

  override def parent(path: String) =
    Option(new File(path).getCanonicalFile().getParentFile()).map(_.getName)

  override def name(path: String) =
    new File(path).getName

  def _list(path: String) =
    new File(path).listFiles.map {
      f ⇒
        val ftype =
          if (Files.isSymbolicLink(f.toPath)) LinkType
          else if (f.isDirectory) DirectoryType
          else FileType

        ListEntry(name = f.getName, `type` = ftype, modificationTime = Some(f.lastModified()))
    }

  def _makeDir(path: String) =
    new File(path).mkdirs

  def _rmDir(path: String) = {
    def delete(f: File): Unit = {
      if (f.isDirectory) f.listFiles.foreach(delete)
      f.delete
    }
    delete(new File(path))
  }

  def _rmFile(path: String) =
    new File(path).delete

  def _mv(from: String, to: String) =
    new File(from).renameTo(new File(to))

  override def _read(path: String): InputStream =
    new FileInputStream(new File(path))

  override def _write(is: InputStream, path: String) =
    Files.copy(is, Paths.get(path), StandardCopyOption.REPLACE_EXISTING)

}

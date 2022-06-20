//> using scala "2.13.8"

package visualcpp

import java.nio.file.{Files, Path, Paths}

import scala.jdk.CollectionConverters._

final case class VisualCpp(
  basePaths: Seq[Path] = Seq(
    Paths.get("C:\\Program Files\\Microsoft Visual Studio"),
    Paths.get("C:\\Program Files (x86)\\Microsoft Visual Studio")
  ),
  versions: Seq[String] = Seq("2022", "2019", "2017"),
  editions: Seq[String] = Seq("Enterprise", "Community", "BuildTools"),
  envVarOpt: Option[String] = Some("VCVARSALL")
) {

  lazy val baseDir: Path = {
    val candidateBaseDirs = {
      val fromEnv = envVarOpt
        .flatMap(k => Option(System.getenv(k)))
        .map(Paths.get(_))
      val lookup = for {
        vsBasePath <- basePaths
        year       <- versions
        edition    <- editions
      } yield vsBasePath.resolve(s"$year/$edition/VC")
      fromEnv ++ lookup
    }
    candidateBaseDirs
      .filter(Files.isDirectory(_))
      .headOption
      .getOrElse {
        sys.error(
          s"No Visual Studio installation found, tried:" + System.lineSeparator() +
            candidateBaseDirs
              .map("  " + _)
              .mkString(System.lineSeparator())
        )
      }
  }

  def findRedist(distRelPath: String = "Redist/MSVC/vc_redist.x64.exe"): Option[Path] =
    Iterator(baseDir)
      .flatMap(Files.list(_).iterator().asScala)
      .filter(Files.isDirectory(_))
      .map(_.resolve(distRelPath))
      .filter(Files.isRegularFile(_))
      .take(1)
      .toList
      .headOption

  lazy val redist: Path =
    findRedist().getOrElse {
      throw new NoSuchElementException(s"${VisualCpp.redistName} under a sub-directory of $baseDir")
    }

  lazy val vcvarsScript: Path = {
    val p = baseDir.resolve("Auxiliary/Build/vcvars64.bat")
    if (!Files.isRegularFile(p))
      throw new NoSuchElementException(s"vcvars script $p")
    p
  }

}

object VisualCpp {
  private def redistName = "vc_redist.x64.exe"
}

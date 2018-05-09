package example

import better.files._
import File._
import java.io.{File => JFile}

import sys.process._

import IOUtils._

object Benchmark {
  type Files = Iterable[File]

  def main(args: Array[String]): Unit = {
    val runSeq = 1 to 20 by 2
    val dotcTimings = for (i <- runSeq) yield {
      val g = new GenerateSrcs(filesCount = 100*i, methodsPerClass = 100)
      val srcs = g.generateScalaSrcs()
      GetTimings.runDotc(srcs)
    }
    val scalacTimings = for (i <- runSeq) yield {
      val g = new GenerateSrcs(filesCount = 100*i, methodsPerClass = 100)
      val srcs = g.generateScalaSrcs()
      GetTimings.runScalac(srcs)
    }
    val javacTimings = for (i <- runSeq) yield {
      val g = new GenerateSrcs(filesCount = 100*i, methodsPerClass = 100)
      val srcs = g.generateJavaSrcs()
      GetTimings.runJavac(srcs)
    }
    println(s"dotc,${dotcTimings.mkString(",")}")
    println(s"scalac,${scalacTimings.mkString(",")}")
    println(s"javac,${javacTimings.mkString(",")}")
  }
}

class GenerateSrcs(filesCount: Int, methodsPerClass: Int) {
  import GenerateSrcs._
  import Benchmark.Files
  def generateJavaSrcs(): Files = {
    for (i <- 1 to filesCount) yield {
      val src = generateClassJavaSrc(s"A$i")
      val file = javaSrcsDir/s"A$i.java"
      file.overwrite(src)
    }
  }

  def generateScalaSrcs(): Files = {
    for (i <- 1 to filesCount) yield {
      val src = generateClassScalaSrc(s"A$i")
      val file = scalaSrcsDir/s"A$i.scala"
      file.overwrite(src)
    }
  }

  def generateClassJavaSrc(name: String): String = s"""|
    |class $name {
    |  ${(for (i <- 1 to methodsPerClass) yield s"String foo$i(String s) { return s; }").mkString("\n")}
    |}
    |""".stripMargin

  def generateClassScalaSrc(name: String): String = s"""|
    |class $name {
    |  ${(for (i <- 1 to methodsPerClass) yield s"def foo$i(s: String): String = s").mkString("\n")}
    |}
    |""".stripMargin
}
object GenerateSrcs {
  val scalaSrcsDir = freshDir(wd/"target"/"scala_srcs")
  val javaSrcsDir = freshDir(wd/"target"/"java_srcs")
}

object GetTimings {
  import Benchmark.Files

  private def timedRun(cmd: String): Double = {
//    println(s"timing $cmd")
    val lines = runAndReturnStdErr(s"time -p $cmd")
    val timings = parseTimeOutput(lines)
    timings("real").toDouble
  }

  def runJavac(srcs: Files): Double = {
    File.temporaryDirectory() { tmpDir =>
        timedRun(s"javac -d $tmpDir ${srcs.mkString(" ")}")
    }
  }

  def runScalac(srcs: Files): Double = {
    File.temporaryDirectory() { tmpDir =>
      val tmpDir = freshDir(wd / "scalac-out")
      timedRun(s"scalac -J-Xmx2g -d $tmpDir ${srcs.mkString(" ")}")
    }
  }

  def runDotc(srcs: Files): Double = {
    File.temporaryDirectory() { tmpDir =>
      timedRun(s"dotc -d $tmpDir ${srcs.mkString(" ")}")
    }
  }

  def runAndReturnStdErr(cmd: String): Seq[String] = {
    val stderr = scala.collection.mutable.Buffer.empty[String]
    cmd ! ProcessLogger(_ => println, stderr append _)
    stderr.foreach(println)
    println("--")
    stderr
  }

  def parseTimeOutput(lines: Seq[String]): Map[String, String] = {
    import scala.util.matching.Regex._
    val pattern = """(\p{Alnum}+)\s+(.+)""".r
    (lines.collect {
      case pattern(name, value) => (name, value)
    }).toMap
  }
}

object IOUtils {
  val wd = System.getProperty("user.dir")
  def freshDir(f: File): File = {
    if (f.exists) f.delete()
    f.createDirectory()
  }
}